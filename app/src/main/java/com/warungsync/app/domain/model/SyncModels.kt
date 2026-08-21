package com.warungsync.app.domain.model

import java.net.InetAddress

data class PeerInfo(
    val deviceId: String,
    val deviceName: String,
    val host: InetAddress,
    val port: Int
)

data class SyncResult(
    val success: Boolean = true,
    val categoriesInserted: Int = 0,
    val categoriesUpdated: Int = 0,
    val itemsInserted: Int = 0,
    val itemsUpdated: Int = 0,
    val historiesInserted: Int = 0,
    val message: String = ""
)

data class SyncStatus(
    val isServerRunning: Boolean = false,
    val connectedPeersCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val isSyncing: Boolean = false,
    val lastError: String? = null
)
