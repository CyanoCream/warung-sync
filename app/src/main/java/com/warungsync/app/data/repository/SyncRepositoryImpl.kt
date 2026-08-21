package com.warungsync.app.data.repository

import android.util.Log
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.dao.CategoryDao
import com.warungsync.app.data.local.dao.ItemDao
import com.warungsync.app.data.local.dao.PriceHistoryDao
import com.warungsync.app.data.local.dao.TokoDao
import com.warungsync.app.data.local.dao.TokoMemberDao
import com.warungsync.app.data.mapper.toDto
import com.warungsync.app.data.mapper.toEntity
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.SyncResult
import com.warungsync.app.domain.repository.SyncRepository
import com.warungsync.app.network.dto.SyncPayloadDto
import com.warungsync.app.network.dto.TokoDto
import com.warungsync.app.network.dto.TokoMemberDto

class SyncRepositoryImpl(
    private val tokoDao: TokoDao,
    private val tokoMemberDao: TokoMemberDao,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val prefs: DevicePreferences
) : SyncRepository {

    override suspend fun saveJoinedToko(toko: TokoDto, member: TokoMemberDto): Result<Unit> {
        return try {
            require(member.tokoId == toko.id) { "Data member tidak cocok dengan toko" }
            require(member.deviceId == prefs.deviceId) { "Identitas member tidak cocok dengan perangkat ini" }
            require(member.isActive) { "Keanggotaan perangkat ini tidak aktif" }

            // Toko dan member lokal harus tersedia sebelum UI berpindah halaman.
            // getMyTokos() memakai INNER JOIN pada kedua tabel ini.
            tokoDao.upsertToko(toko.toEntity())
            tokoMemberDao.upsertMember(member.toEntity())
            prefs.activeTokoId = toko.id
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save joined toko ${toko.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun createSyncPayload(tokoId: String, since: Long): SyncPayloadDto {
        val myRole = tokoMemberDao.getMyRole(tokoId, prefs.deviceId) ?: "USER"
        val toko = tokoDao.getTokoById(tokoId)?.toDto()
        val members = tokoMemberDao.getMembersForToko(tokoId).map { it.toDto() }
        val categories = categoryDao.getCategoriesModifiedSince(tokoId, since).map { it.toDto() }
        val items = itemDao.getItemsModifiedSince(tokoId, since).map { it.toDto() }
        val histories = priceHistoryDao.getHistoriesModifiedSince(tokoId, since).map { it.toDto() }

        return SyncPayloadDto(
            tokoId = tokoId,
            deviceId = prefs.deviceId,
            deviceName = prefs.deviceName.ifBlank { "Unknown Device" },
            senderRole = myRole,
            toko = toko,
            members = members,
            categories = categories,
            items = items,
            histories = histories,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun applySyncPayload(payload: SyncPayloadDto): Result<SyncResult> {
        return try {
            val tokoId = payload.tokoId

            // 1. Cek / Sinkronkan data Toko & Member terlebih dahulu
            if (payload.toko != null) {
                val localToko = tokoDao.getTokoById(tokoId)
                if (localToko == null) {
                    tokoDao.upsertToko(payload.toko.toEntity())
                } else if (payload.toko.updatedAt > localToko.updatedAt) {
                    tokoDao.upsertToko(payload.toko.toEntity())
                }
            }

            // Sync Member records (LWW)
            for (memberDto in payload.members) {
                val localMember = tokoMemberDao.getMemberByDevice(tokoId, memberDto.deviceId)
                if (localMember == null) {
                    tokoMemberDao.upsertMember(memberDto.toEntity())
                } else if (memberDto.updatedAt > localMember.updatedAt) {
                    tokoMemberDao.upsertMember(memberDto.toEntity())
                }
            }

            // Cek role dari pengirim payload di lokal kita
            val senderMember = tokoMemberDao.getMemberByDevice(tokoId, payload.deviceId)
            val senderRoleStr = senderMember?.role ?: payload.senderRole
            val senderRole = MemberRole.fromString(senderRoleStr)

            // JIKA SENDER ADALAH USER BIASA: Tolak semua data modifikasi barang & kategori
            // User hanya boleh download/pull data, bukan push data perubahan ke orang lain
            if (senderRole == MemberRole.USER && payload.categories.isEmpty() && payload.items.isEmpty()) {
                return Result.success(
                    SyncResult(
                        success = true,
                        categoriesInserted = 0,
                        categoriesUpdated = 0,
                        itemsInserted = 0,
                        itemsUpdated = 0,
                        historiesInserted = 0,
                        message = "Pull sync completed for viewer"
                    )
                )
            }

            var catInserted = 0
            var catUpdated = 0
            var itemInserted = 0
            var itemUpdated = 0
            var historyInserted = 0

            // 2. Process Categories with Role & Demotion Timestamp Check
            for (catDto in payload.categories) {
                // Cek siapa yang terakhir update kategori ini
                val updaterDeviceId = catDto.updatedByDevice
                val updaterMember = tokoMemberDao.getMemberByDevice(tokoId, updaterDeviceId)

                // Proteksi Demotion: jika pembuat update adalah USER, cek kapan dia di-demote
                if (updaterMember != null && updaterMember.role == "USER") {
                    if (catDto.updatedAt > updaterMember.roleChangedAt) {
                        Log.w(TAG, "Rejected category update '${catDto.namaKategori}' from demoted user: ${updaterMember.deviceName}")
                        continue
                    }
                }

                val localCat = categoryDao.getCategoryById(catDto.id)
                if (localCat == null) {
                    categoryDao.upsertCategory(catDto.toEntity())
                    catInserted++
                } else if (catDto.updatedAt > localCat.updatedAt) {
                    categoryDao.upsertCategory(catDto.toEntity())
                    catUpdated++
                }
            }

            // 3. Process Items with Role & Demotion Timestamp Check
            for (itemDto in payload.items) {
                // Cek siapa yang terakhir update item ini
                val updaterDeviceId = itemDto.updatedByDevice
                val updaterMember = tokoMemberDao.getMemberByDevice(tokoId, updaterDeviceId)

                // Proteksi Demotion: jika pembuat update adalah USER, cek kapan dia di-demote
                if (updaterMember != null && updaterMember.role == "USER") {
                    if (itemDto.updatedAt > updaterMember.roleChangedAt) {
                        Log.w(TAG, "Rejected item update '${itemDto.namaBarang}' from demoted user: ${updaterMember.deviceName}")
                        continue
                    }
                }

                val localItem = itemDao.getRawItemById(itemDto.id)
                if (localItem == null) {
                    itemDao.upsertItem(itemDto.toEntity())
                    itemInserted++
                } else if (itemDto.updatedAt > localItem.updatedAt) {
                    itemDao.upsertItem(itemDto.toEntity())
                    itemUpdated++
                }
            }

            // 4. Process Price History
            for (histDto in payload.histories) {
                val updaterMember = tokoMemberDao.getMemberByDevice(tokoId, histDto.changedByDevice)
                if (updaterMember != null && updaterMember.role == "USER") {
                    if (histDto.changedAt > updaterMember.roleChangedAt) {
                        continue
                    }
                }

                priceHistoryDao.insertHistory(histDto.toEntity())
                historyInserted++
            }

            // Update timestamp sync terakhir
            prefs.lastSyncTimestamp = System.currentTimeMillis()

            Result.success(
                SyncResult(
                    success = true,
                    categoriesInserted = catInserted,
                    categoriesUpdated = catUpdated,
                    itemsInserted = itemInserted,
                    itemsUpdated = itemUpdated,
                    historiesInserted = historyInserted,
                    message = "Sync berhasil: $itemInserted item baru, $itemUpdated diperbarui"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying sync payload", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SyncRepositoryImpl"
    }
}
