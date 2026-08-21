package com.warungsync.app.domain.repository

import com.warungsync.app.domain.model.SyncResult
import com.warungsync.app.network.dto.SyncPayloadDto

interface SyncRepository {
    suspend fun createSyncPayload(tokoId: String, since: Long): SyncPayloadDto
    suspend fun applySyncPayload(payload: SyncPayloadDto): Result<SyncResult>
}
