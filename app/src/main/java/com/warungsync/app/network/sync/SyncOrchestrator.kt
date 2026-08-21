package com.warungsync.app.network.sync

import android.util.Log
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.network.discovery.NsdDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SyncOrchestrator(
    private val nsdManager: NsdDiscoveryManager,
    private val syncServer: SyncServer,
    private val syncClient: SyncClient,
    private val prefs: DevicePreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicSyncJob: Job? = null
    private var launchSyncJob: Job? = null
    private var isRunning = false

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<String?>(null)
    val lastSyncResult: StateFlow<String?> = _lastSyncResult.asStateFlow()

    fun start() {
        if (isRunning) return
        isRunning = true

        // Start Ktor HTTP Server
        syncServer.start(PORT)

        // Register NSD service & start discovery
        val serviceName = "WarungSync-${prefs.deviceId.take(8)}"
        nsdManager.registerService(serviceName, PORT)
        nsdManager.startDiscovery()

        // Schedule periodic sync every 5 hours (5 * 60 * 60 * 1000 ms)
        startPeriodicSync()

        // Exactly one automatic sync per app launch, as soon as the first peer
        // is discoverable. Further peer-list churn must not start more work.
        launchSyncJob?.cancel()
        launchSyncJob = scope.launch {
            nsdManager.discoveredPeers.first { it.isNotEmpty() }
            if (isRunning) triggerSync()
        }
    }

    private fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (isActive) {
                val fiveHoursMillis = 5 * 60 * 60 * 1000L
                val timeSinceLastSync = System.currentTimeMillis() - prefs.lastSyncTimestamp

                if (timeSinceLastSync >= fiveHoursMillis) {
                    Log.i(TAG, "5 hours passed since last sync. Running auto-sync...")
                    triggerSync()
                }

                // Check again in 15 minutes
                delay(15 * 60 * 1000L)
            }
        }
    }

    fun triggerSync() {
        val activeTokoId = prefs.activeTokoId
        if (activeTokoId.isNullOrBlank()) {
            Log.d(TAG, "No active toko selected for sync")
            return
        }

        val peers = nsdManager.discoveredPeers.value
        if (peers.isEmpty()) {
            Log.d(TAG, "No peers discovered for sync")
            return
        }

        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        scope.launch {
            _isSyncing.value = true
            var anySuccess = false
            val results = mutableListOf<String>()

            for (peer in peers) {
                try {
                    val result = syncClient.syncWithPeer(activeTokoId, peer.hostAddress, peer.port)
                    result.fold(
                        onSuccess = { syncResult ->
                            anySuccess = true
                            results.add("${peer.serviceName}: ${syncResult.message}")
                        },
                        onFailure = { error ->
                            results.add("${peer.serviceName}: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error with peer ${peer.hostAddress}", e)
                }
            }

            _isSyncing.value = false
            _lastSyncResult.value = if (anySuccess) {
                results.joinToString("; ")
            } else {
                "Sync selesai (tidak ada data baru)"
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        periodicSyncJob?.cancel()
        launchSyncJob?.cancel()

        nsdManager.stopDiscovery()
        nsdManager.unregisterService()
        syncServer.stop()
        syncClient.close()
    }

    companion object {
        private const val TAG = "SyncOrchestrator"
        const val PORT = 8080
    }
}
