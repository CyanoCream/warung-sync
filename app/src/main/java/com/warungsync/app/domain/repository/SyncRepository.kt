package com.warungsync.app.domain.repository

import com.warungsync.app.domain.model.SyncResult
import com.warungsync.app.network.dto.SyncPayloadDto
import com.warungsync.app.network.dto.TokoDto
import com.warungsync.app.network.dto.TokoMemberDto

interface SyncRepository {
    suspend fun createSyncPayload(tokoId: String, since: Long): SyncPayloadDto
    suspend fun applySyncPayload(payload: SyncPayloadDto): Result<SyncResult>
    suspend fun saveJoinedToko(toko: TokoDto, member: TokoMemberDto): Result<Unit>
}
