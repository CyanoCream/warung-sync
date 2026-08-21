package com.warungsync.app.network.sync

import android.util.Log
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.domain.model.SyncResult
import com.warungsync.app.domain.repository.SyncRepository
import com.warungsync.app.network.dto.DeviceInfoDto
import com.warungsync.app.network.dto.JoinRequestDto
import com.warungsync.app.network.dto.JoinResponseDto
import com.warungsync.app.network.dto.SyncPayloadDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SyncClient(
    private val syncRepository: SyncRepository,
    private val prefs: DevicePreferences
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    suspend fun fetchDeviceInfo(peerIp: String, peerPort: Int = 8080): Result<DeviceInfoDto> {
        return try {
            val response: DeviceInfoDto = client.get("http://$peerIp:$peerPort/info").body()
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch device info from $peerIp:$peerPort", e)
            Result.failure(e)
        }
    }

    suspend fun requestJoinToko(tokoId: String, peerIp: String, peerPort: Int = 8080): Result<JoinResponseDto> {
        return try {
            val request = JoinRequestDto(
                deviceId = prefs.deviceId,
                deviceName = prefs.deviceName.ifBlank { "User Device" }
            )
            val response: JoinResponseDto = client.post("http://$peerIp:$peerPort/toko/$tokoId/join") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.success) {
                val toko = response.toko
                    ?: return Result.failure(IllegalStateException("Respons toko dari perangkat pemilik tidak lengkap"))
                val member = response.member
                    ?: return Result.failure(IllegalStateException("Respons member dari perangkat pemilik tidak lengkap"))

                val saveResult = syncRepository.saveJoinedToko(toko, member)
                if (saveResult.isFailure) {
                    return Result.failure(
                        saveResult.exceptionOrNull()
                            ?: IllegalStateException("Gagal menyimpan toko yang baru digabung")
                    )
                }

                // Toko baru wajib menarik seluruh data sejak awal. Timestamp sync global
                // dari toko lain tidak boleh membuat produk lama terlewat.
                syncWithPeer(toko.id, peerIp, peerPort, since = 0L)
                    .onFailure { error ->
                        Log.w(TAG, "Joined toko ${toko.id}, but initial full sync will be retried later", error)
                    }
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join toko $tokoId on $peerIp", e)
            Result.failure(e)
        }
    }

    suspend fun syncWithPeer(
        tokoId: String,
        peerIp: String,
        peerPort: Int = 8080,
        since: Long = prefs.lastSyncTimestamp
    ): Result<SyncResult> {
        return try {
            Log.d(TAG, "Starting 2-way sync for toko $tokoId with peer $peerIp:$peerPort")

            // Step 1: PULL changes from peer
            val remotePayload: SyncPayloadDto = client.get("http://$peerIp:$peerPort/sync/$tokoId") {
                parameter("since", since)
            }.body()

            val pullResult = syncRepository.applySyncPayload(remotePayload)
            if (pullResult.isFailure) {
                return pullResult
            }

            // Step 2: PUSH local changes to peer
            val localPayload = syncRepository.createSyncPayload(tokoId, since)
            val pushResponse: SyncResult = client.post("http://$peerIp:$peerPort/sync/$tokoId") {
                contentType(ContentType.Application.Json)
                setBody(localPayload)
            }.body()

            val combined = SyncResult(
                success = true,
                categoriesInserted = (pullResult.getOrNull()?.categoriesInserted ?: 0) + pushResponse.categoriesInserted,
                categoriesUpdated = (pullResult.getOrNull()?.categoriesUpdated ?: 0) + pushResponse.categoriesUpdated,
                itemsInserted = (pullResult.getOrNull()?.itemsInserted ?: 0) + pushResponse.itemsInserted,
                itemsUpdated = (pullResult.getOrNull()?.itemsUpdated ?: 0) + pushResponse.itemsUpdated,
                historiesInserted = (pullResult.getOrNull()?.historiesInserted ?: 0) + pushResponse.historiesInserted,
                message = "Sinkronisasi selesai dengan $peerIp"
            )

            Result.success(combined)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with $peerIp:$peerPort for toko $tokoId", e)
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        private const val TAG = "SyncClient"
    }
}
