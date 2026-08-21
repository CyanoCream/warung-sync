package com.warungsync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.warungsync.app.data.local.entity.TokoEntity
import kotlinx.coroutines.flow.Flow

data class TokoWithRole(
    val id: String,
    val namaToko: String,
    val ownerDeviceId: String,
    val ownerDeviceName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val myRole: String,
    val memberCount: Int
)

@Dao
interface TokoDao {

    @Query("""
        SELECT t.id, t.namaToko, t.ownerDeviceId, t.ownerDeviceName, t.createdAt, t.updatedAt,
               m.role AS myRole,
               (SELECT COUNT(*) FROM toko_members WHERE tokoId = t.id AND isActive = 1) AS memberCount
        FROM tokos t
        INNER JOIN toko_members m ON t.id = m.tokoId
        WHERE m.deviceId = :deviceId AND m.isActive = 1 AND t.isDeleted = 0
        ORDER BY t.updatedAt DESC
    """)
    fun getMyTokos(deviceId: String): Flow<List<TokoWithRole>>

    @Query("SELECT * FROM tokos WHERE id = :id AND isDeleted = 0")
    suspend fun getTokoById(id: String): TokoEntity?

    @Query("SELECT * FROM tokos WHERE isDeleted = 0")
    suspend fun getAllActiveTokos(): List<TokoEntity>

    @Query("SELECT * FROM tokos WHERE isDeleted = 0")
    fun getAllActiveTokosFlow(): Flow<List<TokoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertToko(toko: TokoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTokos(tokos: List<TokoEntity>)

    @Query("UPDATE tokos SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteToko(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tokos SET namaToko = :namaToko, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateNamaToko(id: String, namaToko: String, timestamp: Long = System.currentTimeMillis())
}
