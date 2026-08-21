package com.warungsync.app.domain.repository

import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import kotlinx.coroutines.flow.Flow

interface TokoRepository {
    fun getMyTokos(): Flow<List<Toko>>
    suspend fun getTokoById(id: String): Toko?
    suspend fun createToko(namaToko: String): Result<Toko>
    suspend fun updateNamaToko(tokoId: String, namaToko: String): Result<Unit>
    suspend fun deleteToko(tokoId: String): Result<Unit>
    suspend fun leaveToko(tokoId: String): Result<Unit>
    
    fun getMembersForToko(tokoId: String): Flow<List<TokoMember>>
    suspend fun getMyRole(tokoId: String): MemberRole
    fun getMyRoleFlow(tokoId: String): Flow<MemberRole>
    suspend fun updateMemberRole(tokoId: String, memberDeviceId: String, newRole: MemberRole): Result<Unit>
    suspend fun kickMember(tokoId: String, memberDeviceId: String): Result<Unit>
}
