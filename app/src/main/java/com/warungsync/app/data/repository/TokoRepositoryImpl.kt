package com.warungsync.app.data.repository

import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.dao.TokoDao
import com.warungsync.app.data.local.dao.TokoMemberDao
import com.warungsync.app.data.local.entity.TokoEntity
import com.warungsync.app.data.local.entity.TokoMemberEntity
import com.warungsync.app.data.mapper.toDomain
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.domain.repository.TokoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class TokoRepositoryImpl(
    private val tokoDao: TokoDao,
    private val tokoMemberDao: TokoMemberDao,
    private val prefs: DevicePreferences
) : TokoRepository {

    override fun getMyTokos(): Flow<List<Toko>> {
        return tokoDao.getMyTokos(prefs.deviceId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTokoById(id: String): Toko? {
        val entity = tokoDao.getTokoById(id) ?: return null
        val myRoleStr = tokoMemberDao.getMyRole(id, prefs.deviceId) ?: return null
        val memberCount = tokoMemberDao.getMembersForToko(id).size
        return Toko(
            id = entity.id,
            namaToko = entity.namaToko,
            ownerDeviceId = entity.ownerDeviceId,
            ownerDeviceName = entity.ownerDeviceName,
            myRole = MemberRole.fromString(myRoleStr),
            memberCount = memberCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun createToko(namaToko: String): Result<Toko> {
        val cleanName = namaToko.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama toko tidak boleh kosong"))
        }

        val now = System.currentTimeMillis()
        val tokoId = UUID.randomUUID().toString()
        val memberId = UUID.randomUUID().toString()

        val tokoEntity = TokoEntity(
            id = tokoId,
            namaToko = cleanName,
            ownerDeviceId = prefs.deviceId,
            ownerDeviceName = prefs.deviceName.ifBlank { "Owner" },
            createdAt = now,
            updatedAt = now
        )

        val memberEntity = TokoMemberEntity(
            id = memberId,
            tokoId = tokoId,
            deviceId = prefs.deviceId,
            deviceName = prefs.deviceName.ifBlank { "Owner" },
            role = MemberRole.OWNER.name,
            roleChangedAt = now,
            joinedAt = now,
            updatedAt = now,
            isActive = true
        )

        tokoDao.upsertToko(tokoEntity)
        tokoMemberDao.upsertMember(memberEntity)
        prefs.incrementCreatedTokoCount()
        prefs.activeTokoId = tokoId

        return Result.success(
            Toko(
                id = tokoId,
                namaToko = cleanName,
                ownerDeviceId = prefs.deviceId,
                ownerDeviceName = prefs.deviceName.ifBlank { "Owner" },
                myRole = MemberRole.OWNER,
                memberCount = 1,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateNamaToko(tokoId: String, namaToko: String): Result<Unit> {
        val myRole = getMyRole(tokoId)
        if (myRole != MemberRole.OWNER) {
            return Result.failure(IllegalStateException("Hanya pemilik toko yang dapat mengubah nama toko"))
        }
        val cleanName = namaToko.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama toko tidak boleh kosong"))
        }
        tokoDao.updateNamaToko(tokoId, cleanName)
        return Result.success(Unit)
    }

    override suspend fun deleteToko(tokoId: String): Result<Unit> {
        val myRole = getMyRole(tokoId)
        if (myRole != MemberRole.OWNER) {
            return Result.failure(IllegalStateException("Hanya pemilik toko yang dapat menghapus toko"))
        }
        tokoDao.softDeleteToko(tokoId)
        if (prefs.activeTokoId == tokoId) {
            prefs.activeTokoId = null
        }
        return Result.success(Unit)
    }

    override suspend fun leaveToko(tokoId: String): Result<Unit> {
        val myRole = getMyRole(tokoId)
        if (myRole == MemberRole.OWNER) {
            return Result.failure(IllegalStateException("Pemilik toko tidak bisa leave, gunakan hapus toko"))
        }
        tokoMemberDao.deactivateMember(tokoId, prefs.deviceId)
        if (prefs.activeTokoId == tokoId) {
            prefs.activeTokoId = null
        }
        return Result.success(Unit)
    }

    override fun getMembersForToko(tokoId: String): Flow<List<TokoMember>> {
        return tokoMemberDao.getMembersForTokoFlow(tokoId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMyRole(tokoId: String): MemberRole {
        val roleStr = tokoMemberDao.getMyRole(tokoId, prefs.deviceId)
        return if (roleStr != null) MemberRole.fromString(roleStr) else MemberRole.USER
    }

    override fun getMyRoleFlow(tokoId: String): Flow<MemberRole> {
        return tokoMemberDao.getMyRoleFlow(tokoId, prefs.deviceId).map { roleStr ->
            if (roleStr != null) MemberRole.fromString(roleStr) else MemberRole.USER
        }
    }

    override suspend fun updateMemberRole(
        tokoId: String,
        memberDeviceId: String,
        newRole: MemberRole
    ): Result<Unit> {
        val myRole = getMyRole(tokoId)
        if (myRole != MemberRole.OWNER) {
            return Result.failure(IllegalStateException("Hanya pemilik toko yang dapat mengubah role member"))
        }
        if (newRole == MemberRole.OWNER) {
            return Result.failure(IllegalArgumentException("Role OWNER tidak dapat dialihkan lewat fitur ini"))
        }
        tokoMemberDao.updateMemberRole(tokoId, memberDeviceId, newRole.name)
        return Result.success(Unit)
    }

    override suspend fun kickMember(tokoId: String, memberDeviceId: String): Result<Unit> {
        val myRole = getMyRole(tokoId)
        if (myRole != MemberRole.OWNER) {
            return Result.failure(IllegalStateException("Hanya pemilik toko yang dapat mengeluarkan member"))
        }
        tokoMemberDao.deactivateMember(tokoId, memberDeviceId)
        return Result.success(Unit)
    }
}
