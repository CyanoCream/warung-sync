package com.warungsync.app.domain.usecase.sync

import com.warungsync.app.domain.model.SyncResult
import com.warungsync.app.network.sync.SyncClient
import com.warungsync.app.network.sync.SyncServer

class SyncWithPeerUseCase(
    private val syncClient: SyncClient,
    private val syncServer: SyncServer
) {
    suspend operator fun invoke(tokoId: String, peerIp: String, peerPort: Int = 8080): Result<SyncResult> {
        return syncClient.syncWithPeer(tokoId, peerIp, peerPort)
    }

    fun startServer(port: Int = 8080) {
        syncServer.start(port)
    }

    fun stopServer() {
        syncServer.stop()
    }
}
