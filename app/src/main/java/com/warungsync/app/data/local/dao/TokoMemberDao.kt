package com.warungsync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.warungsync.app.data.local.entity.TokoMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TokoMemberDao {

    @Query("SELECT * FROM toko_members WHERE tokoId = :tokoId AND isActive = 1 ORDER BY joinedAt ASC")
    fun getMembersForTokoFlow(tokoId: String): Flow<List<TokoMemberEntity>>

    @Query("SELECT * FROM toko_members WHERE tokoId = :tokoId AND isActive = 1")
    suspend fun getMembersForToko(tokoId: String): List<TokoMemberEntity>

    @Query("SELECT * FROM toko_members WHERE tokoId = :tokoId AND deviceId = :deviceId LIMIT 1")
    suspend fun getMemberByDevice(tokoId: String, deviceId: String): TokoMemberEntity?

    @Query("SELECT role FROM toko_members WHERE tokoId = :tokoId AND deviceId = :deviceId AND isActive = 1 LIMIT 1")
    suspend fun getMyRole(tokoId: String, deviceId: String): String?

    @Query("SELECT role FROM toko_members WHERE tokoId = :tokoId AND deviceId = :deviceId AND isActive = 1 LIMIT 1")
    fun getMyRoleFlow(tokoId: String, deviceId: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: TokoMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<TokoMemberEntity>)

    @Query("""
        UPDATE toko_members 
        SET role = :newRole, roleChangedAt = :timestamp, updatedAt = :timestamp 
        WHERE tokoId = :tokoId AND deviceId = :deviceId
    """)
    suspend fun updateMemberRole(tokoId: String, deviceId: String, newRole: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE toko_members SET isActive = 0, updatedAt = :timestamp WHERE tokoId = :tokoId AND deviceId = :deviceId")
    suspend fun deactivateMember(tokoId: String, deviceId: String, timestamp: Long = System.currentTimeMillis())
}
