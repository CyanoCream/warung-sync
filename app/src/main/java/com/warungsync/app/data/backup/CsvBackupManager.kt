package com.warungsync.app.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.WarungSyncDatabase
import com.warungsync.app.data.local.entity.CategoryEntity
import com.warungsync.app.data.local.entity.ItemEntity
import com.warungsync.app.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class ExportDataResult(
    val fileName: String,
    val categoryCount: Int,
    val itemCount: Int,
    val historyCount: Int
)

data class ImportDataResult(
    val fileName: String,
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int
)

data class BackupMetadata(
    val fileName: String,
    val sourceTokoName: String
)

private data class ParsedBackup(
    val sourceTokoId: String,
    val sourceTokoName: String,
    val records: List<Map<String, String>>
)

/** CSV backup that remains readable by spreadsheet apps while preserving sync metadata. */
class CsvBackupManager(
    context: Context,
    private val database: WarungSyncDatabase,
    private val prefs: DevicePreferences
) {
    private val resolver = context.contentResolver

    suspend fun inspectBackup(source: Uri): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = readBackup(source)
            BackupMetadata(
                fileName = displayName(source),
                sourceTokoName = parsed.sourceTokoName.ifBlank { "Toko Dipulihkan" }
            )
        }
    }

    suspend fun exportToko(tokoId: String, destination: Uri): Result<ExportDataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val toko = database.tokoDao().getTokoById(tokoId)
                    ?: error("Toko tidak ditemukan")
                val categories = database.categoryDao()
                    .getCategoriesModifiedSince(tokoId, -1L)
                    .sortedBy { it.namaKategori.lowercase() }
                val items = database.itemDao()
                    .getItemsModifiedSince(tokoId, -1L)
                    .sortedBy { it.namaBarang.lowercase() }
                val histories = database.priceHistoryDao()
                    .getHistoriesModifiedSince(tokoId, -1L)
                    .sortedBy { it.changedAt }

                val rows = buildList {
                    add(COLUMNS)
                    add(
                        backupRow(
                            RECORD_META,
                            "format_version" to FORMAT_VERSION.toString(),
                            "source_toko_id" to toko.id,
                            "source_toko_name" to toko.namaToko,
                            "exported_at" to System.currentTimeMillis().toString()
                        )
                    )
                    categories.forEach { category ->
                        add(
                            backupRow(
                                RECORD_CATEGORY,
                                "id" to category.id,
                                "name" to category.namaKategori,
                                "color_argb" to category.colorArgb.toString(),
                                "updated_at" to category.updatedAt.toString(),
                                "updated_by_device" to category.updatedByDevice,
                                "is_deleted" to category.isDeleted.toString()
                            )
                        )
                    }
                    items.forEach { item ->
                        add(
                            backupRow(
                                RECORD_ITEM,
                                "id" to item.id,
                                "name" to item.namaBarang,
                                "description" to item.deskripsi.orEmpty(),
                                "price" to item.harga.toString(),
                                "unit_quantity" to item.unitQuantity.toString(),
                                "unit" to item.satuan,
                                "category_id" to item.categoryId,
                                "category_ids" to item.categoryIdsCsv.replace(',', '|'),
                                "updated_at" to item.updatedAt.toString(),
                                "updated_by_device" to item.updatedByDevice,
                                "is_deleted" to item.isDeleted.toString()
                            )
                        )
                    }
                    histories.forEach { history ->
                        add(
                            backupRow(
                                RECORD_HISTORY,
                                "id" to history.id,
                                "item_id" to history.itemId,
                                "old_price" to history.hargaLama.toString(),
                                "new_price" to history.hargaBaru.toString(),
                                "old_unit" to history.satuanLama,
                                "new_unit" to history.satuanBaru,
                                "changed_at" to history.changedAt.toString(),
                                "changed_by_device" to history.changedByDevice
                            )
                        )
                    }
                }

                resolver.openOutputStream(destination)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write('\uFEFF'.code)
                    rows.forEach { row ->
                        writer.appendLine(CsvCodec.encodeRow(row))
                    }
                } ?: error("File tujuan tidak dapat dibuka")

                ExportDataResult(
                    fileName = displayName(destination),
                    categoryCount = categories.count { !it.isDeleted },
                    itemCount = items.count { !it.isDeleted },
                    historyCount = histories.size
                )
            }
        }

    suspend fun importIntoToko(tokoId: String, source: Uri): Result<ImportDataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                database.tokoDao().getTokoById(tokoId) ?: error("Toko tujuan tidak ditemukan")
                val parsed = readBackup(source)
                val records = parsed.records
                val sourceTokoId = parsed.sourceTokoId
                val sameToko = sourceTokoId == tokoId

                val categoryRows = records.filter { it.value("record_type") == RECORD_CATEGORY }
                val itemRows = records.filter { it.value("record_type") == RECORD_ITEM }
                val historyRows = records.filter { it.value("record_type") == RECORD_HISTORY }

                var inserted = 0
                var updated = 0
                var skipped = 0
                val categoryIdMap = mutableMapOf<String, String>()
                val itemIdMap = mutableMapOf<String, String>()

                database.withTransaction {
                    val categoryDao = database.categoryDao()
                    val itemDao = database.itemDao()
                    val historyDao = database.priceHistoryDao()

                    categoryRows.forEach { row ->
                        val sourceId = row.required("id")
                        val name = row.required("name").trim()
                        val importedAt = row.requiredLong("updated_at")
                        val isDeleted = row.value("is_deleted").toBooleanStrictOrNull() ?: false
                        val desiredId = if (sameToko) sourceId else remappedId(tokoId, "category", sourceId)
                        val byId = categoryDao.getRawCategoryById(desiredId)?.takeIf { it.tokoId == tokoId }
                        val byName = categoryDao.getRawCategoryByName(tokoId, name)
                        val existing = byId ?: byName

                        if (isDeleted && existing == null) {
                            skipped++
                            return@forEach
                        }

                        val targetId = existing?.id ?: desiredId
                        categoryIdMap[sourceId] = targetId
                        val imported = CategoryEntity(
                            id = targetId,
                            tokoId = tokoId,
                            namaKategori = name,
                            colorArgb = row.value("color_argb").toIntOrNull() ?: DEFAULT_COLOR,
                            updatedAt = importedAt,
                            updatedByDevice = row.value("updated_by_device").ifBlank { prefs.deviceId },
                            isDeleted = isDeleted
                        )
                        when {
                            existing == null -> {
                                categoryDao.upsertCategory(imported)
                                inserted++
                            }
                            importedAt > existing.updatedAt -> {
                                categoryDao.upsertCategory(imported)
                                updated++
                            }
                            else -> skipped++
                        }
                    }

                    itemRows.forEach { row ->
                        val sourceId = row.required("id")
                        val name = row.required("name").trim()
                        val importedAt = row.requiredLong("updated_at")
                        val isDeleted = row.value("is_deleted").toBooleanStrictOrNull() ?: false
                        val sourceCategoryIds = row.value("category_ids")
                            .split('|')
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .ifEmpty { listOf(row.required("category_id")) }
                            .distinct()
                        val mappedCategoryIds = sourceCategoryIds.mapNotNull(categoryIdMap::get)
                        if (mappedCategoryIds.size != sourceCategoryIds.size) {
                            skipped++
                            return@forEach
                        }
                        val targetCategories = mappedCategoryIds.mapNotNull {
                            categoryDao.getRawCategoryById(it)
                        }
                        if (!isDeleted && (
                                targetCategories.size != mappedCategoryIds.size ||
                                    targetCategories.any { it.isDeleted }
                                )) {
                            skipped++
                            return@forEach
                        }

                        val desiredId = if (sameToko) sourceId else remappedId(tokoId, "item", sourceId)
                        val byId = itemDao.getRawItemById(desiredId)?.takeIf { it.tokoId == tokoId }
                        val byName = itemDao.getRawItemByName(tokoId, name)
                        val existing = byId ?: byName
                        if (isDeleted && existing == null) {
                            skipped++
                            return@forEach
                        }

                        val targetId = existing?.id ?: desiredId
                        itemIdMap[sourceId] = targetId
                        val imported = ItemEntity(
                            id = targetId,
                            tokoId = tokoId,
                            namaBarang = name,
                            deskripsi = row.value("description").ifBlank { null },
                            harga = row.requiredDouble("price"),
                            satuan = row.required("unit"),
                            unitQuantity = row.value("unit_quantity").toDoubleOrNull() ?: 1.0,
                            categoryId = mappedCategoryIds.first(),
                            categoryIdsCsv = mappedCategoryIds.joinToString(","),
                            updatedAt = importedAt,
                            updatedByDevice = row.value("updated_by_device").ifBlank { prefs.deviceId },
                            isDeleted = isDeleted
                        )
                        when {
                            existing == null -> {
                                itemDao.upsertItem(imported)
                                inserted++
                            }
                            importedAt > existing.updatedAt -> {
                                itemDao.upsertItem(imported)
                                updated++
                            }
                            else -> skipped++
                        }
                    }

                    historyRows.forEach { row ->
                        val mappedItemId = itemIdMap[row.required("item_id")]
                        if (mappedItemId == null) {
                            skipped++
                            return@forEach
                        }
                        val sourceId = row.required("id")
                        val targetId = if (sameToko) sourceId else remappedId(tokoId, "history", sourceId)
                        val imported = PriceHistoryEntity(
                            id = targetId,
                            tokoId = tokoId,
                            itemId = mappedItemId,
                            hargaLama = row.requiredDouble("old_price"),
                            hargaBaru = row.requiredDouble("new_price"),
                            satuanLama = row.required("old_unit"),
                            satuanBaru = row.required("new_unit"),
                            changedAt = row.requiredLong("changed_at"),
                            changedByDevice = row.value("changed_by_device").ifBlank { prefs.deviceId }
                        )
                        val existing = historyDao.getHistoryById(targetId)
                        when {
                            existing == null -> {
                                historyDao.insertHistory(imported)
                                inserted++
                            }
                            imported.changedAt > existing.changedAt -> {
                                historyDao.insertHistory(imported)
                                updated++
                            }
                            else -> skipped++
                        }
                    }
                }

                // Force the next user/manual sync to consider imported rows whose
                // original timestamps may predate the last successful sync.
                prefs.lastSyncTimestamp = 0L
                ImportDataResult(
                    fileName = displayName(source),
                    insertedCount = inserted,
                    updatedCount = updated,
                    skippedCount = skipped
                )
            }
        }

    private fun backupRow(type: String, vararg values: Pair<String, String>): List<String> {
        val mapped = values.toMap() + ("record_type" to type)
        return COLUMNS.map { mapped[it].orEmpty() }
    }

    private fun readBackup(source: Uri): ParsedBackup {
        requireFileSizeIsSafe(source)
        val csvText = resolver.openInputStream(source)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: error("File sumber tidak dapat dibuka")
        val decoded = CsvCodec.decode(csvText)
            .filter { row -> row.any { it.isNotBlank() } }
        require(decoded.size >= 2) { "File CSV kosong atau tidak lengkap" }

        val header = decoded.first().mapIndexed { index, value ->
            if (index == 0) value.removePrefix("\uFEFF") else value
        }
        require(REQUIRED_COLUMNS.all { it in header }) {
            "Format CSV bukan backup WarungSync"
        }
        val records = decoded.drop(1).map { values ->
            header.mapIndexed { index, name -> name to values.getOrElse(index) { "" } }.toMap()
        }
        val meta = records.firstOrNull { it.value("record_type") == RECORD_META }
            ?: error("Metadata backup tidak ditemukan")
        val formatVersion = meta.value("format_version").toIntOrNull()
            ?: error("Versi backup tidak valid")
        require(formatVersion in 1..FORMAT_VERSION) { "Versi backup belum didukung" }
        return ParsedBackup(
            sourceTokoId = meta.required("source_toko_id"),
            sourceTokoName = meta.value("source_toko_name"),
            records = records
        )
    }

    private fun displayName(uri: Uri): String {
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: "backup.csv"
    }

    private fun requireFileSizeIsSafe(uri: Uri) {
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
        require(size == null || size <= MAX_BACKUP_BYTES) { "File backup terlalu besar (maksimal 25 MB)" }
    }

    private fun remappedId(tokoId: String, type: String, sourceId: String): String =
        UUID.nameUUIDFromBytes("warungsync:$tokoId:$type:$sourceId".toByteArray(Charsets.UTF_8)).toString()

    private fun Map<String, String>.value(key: String): String = this[key].orEmpty()

    private fun Map<String, String>.required(key: String): String =
        value(key).takeIf { it.isNotBlank() } ?: error("Kolom $key tidak lengkap")

    private fun Map<String, String>.requiredLong(key: String): Long =
        required(key).toLongOrNull() ?: error("Nilai $key tidak valid")

    private fun Map<String, String>.requiredDouble(key: String): Double =
        required(key).toDoubleOrNull() ?: error("Nilai $key tidak valid")

    companion object {
        private const val FORMAT_VERSION = 2
        private const val MAX_BACKUP_BYTES = 25L * 1024L * 1024L
        private const val DEFAULT_COLOR = -11581723
        private const val RECORD_META = "META"
        private const val RECORD_CATEGORY = "CATEGORY"
        private const val RECORD_ITEM = "ITEM"
        private const val RECORD_HISTORY = "HISTORY"

        private val COLUMNS = listOf(
            "record_type",
            "format_version",
            "source_toko_id",
            "source_toko_name",
            "exported_at",
            "id",
            "name",
            "description",
            "price",
            "unit_quantity",
            "unit",
            "category_id",
            "category_ids",
            "color_argb",
            "item_id",
            "old_price",
            "new_price",
            "old_unit",
            "new_unit",
            "changed_at",
            "changed_by_device",
            "updated_at",
            "updated_by_device",
            "is_deleted"
        )
        private val REQUIRED_COLUMNS = listOf(
            "record_type",
            "format_version",
            "source_toko_id",
            "id",
            "name",
            "updated_at"
        )
    }
}

private object CsvCodec {
    fun encodeRow(values: List<String>): String = values.joinToString(",") { value ->
        "\"${value.replace("\"", "\"\"")}\""
    }

    fun decode(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    row += field.toString()
                    field.clear()
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += field.toString()
                    field.clear()
                    rows += row.toList()
                    row.clear()
                }
                else -> field.append(char)
            }
            index++
        }

        require(!quoted) { "Format CSV rusak: tanda kutip tidak berpasangan" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row.toList()
        }
        return rows
    }
}
