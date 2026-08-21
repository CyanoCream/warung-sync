package com.warungsync.app.data.repository

import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.dao.CategoryDao
import com.warungsync.app.data.local.dao.ItemDao
import com.warungsync.app.data.local.dao.PriceHistoryDao
import com.warungsync.app.data.local.entity.ItemEntity
import com.warungsync.app.data.local.entity.PriceHistoryEntity
import com.warungsync.app.data.mapper.toDomain
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.formatUnitQuantity
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ItemRepositoryImpl(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val prefs: DevicePreferences
) : ItemRepository {

    override fun getFilteredItems(tokoId: String, filter: ItemFilter): Flow<List<Item>> {
        return itemDao.getFilteredItems(
            tokoId = tokoId,
            searchQuery = filter.searchQuery.trim(),
            categoryId = filter.categoryId,
            minHarga = filter.minHarga,
            maxHarga = filter.maxHarga,
            sortBy = filter.sortBy.name
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getItemById(id: String): Item? {
        return itemDao.getItemById(id)?.toDomain()
    }

    override suspend fun addItem(
        tokoId: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        unitQuantity: Double,
        satuan: String,
        categoryId: String
    ): Result<Item> {
        val trimmedName = namaBarang.trim()
        val trimmedSatuan = satuan.trim()
        val cleanDeskripsi = deskripsi?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama barang tidak boleh kosong"))
        }
        if (harga <= 0) {
            return Result.failure(IllegalArgumentException("Harga harus lebih besar dari 0"))
        }
        if (unitQuantity <= 0) {
            return Result.failure(IllegalArgumentException("Jumlah/volume harus lebih besar dari 0"))
        }
        if (trimmedSatuan.isBlank()) {
            return Result.failure(IllegalArgumentException("Satuan tidak boleh kosong"))
        }

        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalArgumentException("Kategori tidak valid"))

        val now = System.currentTimeMillis()
        val itemId = UUID.randomUUID().toString()

        val itemEntity = ItemEntity(
            id = itemId,
            tokoId = tokoId,
            namaBarang = trimmedName,
            deskripsi = cleanDeskripsi,
            harga = harga,
            satuan = trimmedSatuan,
            unitQuantity = unitQuantity,
            categoryId = categoryId,
            updatedAt = now,
            updatedByDevice = prefs.deviceId,
            isDeleted = false
        )

        // Insert first price history
        val historyEntity = PriceHistoryEntity(
            id = UUID.randomUUID().toString(),
            tokoId = tokoId,
            itemId = itemId,
            hargaLama = 0.0,
            hargaBaru = harga,
            satuanLama = "-",
            satuanBaru = formatUnitQuantity(unitQuantity, trimmedSatuan),
            changedAt = now,
            changedByDevice = prefs.deviceId
        )

        itemDao.upsertItem(itemEntity)
        priceHistoryDao.insertHistory(historyEntity)

        return Result.success(itemEntity.toDomain(category.namaKategori))
    }

    override suspend fun updateItem(
        tokoId: String,
        id: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        unitQuantity: Double,
        satuan: String,
        categoryId: String
    ): Result<Item> {
        val trimmedName = namaBarang.trim()
        val trimmedSatuan = satuan.trim()
        val cleanDeskripsi = deskripsi?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama barang tidak boleh kosong"))
        }
        if (harga <= 0) {
            return Result.failure(IllegalArgumentException("Harga harus lebih besar dari 0"))
        }
        if (unitQuantity <= 0) {
            return Result.failure(IllegalArgumentException("Jumlah/volume harus lebih besar dari 0"))
        }
        if (trimmedSatuan.isBlank()) {
            return Result.failure(IllegalArgumentException("Satuan tidak boleh kosong"))
        }

        val currentItem = itemDao.getRawItemById(id)
            ?: return Result.failure(NoSuchElementException("Barang tidak ditemukan"))

        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalArgumentException("Kategori tidak valid"))

        val now = System.currentTimeMillis()

        // Catat history jika harga atau satuan berubah
        val isPriceOrSatuanChanged = currentItem.harga != harga ||
            currentItem.satuan != trimmedSatuan ||
            currentItem.unitQuantity != unitQuantity
        if (isPriceOrSatuanChanged) {
            val historyEntity = PriceHistoryEntity(
                id = UUID.randomUUID().toString(),
                tokoId = tokoId,
                itemId = id,
                hargaLama = currentItem.harga,
                hargaBaru = harga,
                satuanLama = formatUnitQuantity(currentItem.unitQuantity, currentItem.satuan),
                satuanBaru = formatUnitQuantity(unitQuantity, trimmedSatuan),
                changedAt = now,
                changedByDevice = prefs.deviceId
            )
            priceHistoryDao.insertHistory(historyEntity)
        }

        val updated = currentItem.copy(
            namaBarang = trimmedName,
            deskripsi = cleanDeskripsi,
            harga = harga,
            satuan = trimmedSatuan,
            unitQuantity = unitQuantity,
            categoryId = categoryId,
            updatedAt = now,
            updatedByDevice = prefs.deviceId
        )

        itemDao.upsertItem(updated)
        return Result.success(updated.toDomain(category.namaKategori))
    }

    override suspend fun deleteItem(id: String): Result<Unit> {
        itemDao.softDeleteItem(id, prefs.deviceId)
        return Result.success(Unit)
    }

    override fun getPriceHistoryForItem(tokoId: String, itemId: String): Flow<List<PriceHistory>> {
        return priceHistoryDao.getHistoryForItem(tokoId, itemId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPriceHistoryBetween(
        tokoId: String,
        itemId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<PriceHistory>> {
        return priceHistoryDao.getPriceHistoryBetween(tokoId, itemId, startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
