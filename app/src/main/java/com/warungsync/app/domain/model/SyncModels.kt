package com.warungsync.app.domain.model

import java.net.InetAddress

data class PeerInfo(
    val deviceId: String,
    val deviceName: String,
    val host: InetAddress,
    val port: Int
)

data class SyncStatus(
    val isServerRunning: Boolean = false,
    val connectedPeersCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val isSyncing: Boolean = false,
    val lastError: String? = null
)
