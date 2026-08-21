package com.warungsync.app.network.sync

import android.util.Log
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.dao.TokoDao
import com.warungsync.app.data.local.dao.TokoMemberDao
import com.warungsync.app.data.local.entity.TokoMemberEntity
import com.warungsync.app.data.mapper.toDto
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.repository.SyncRepository
import com.warungsync.app.network.dto.DeviceInfoDto
import com.warungsync.app.network.dto.JoinRequestDto
import com.warungsync.app.network.dto.JoinResponseDto
import com.warungsync.app.network.dto.SyncPayloadDto
import com.warungsync.app.network.dto.TokoSummaryDto
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class SyncServer(
    private val syncRepository: SyncRepository,
    private val tokoDao: TokoDao,
    private val tokoMemberDao: TokoMemberDao,
    private val prefs: DevicePreferences
) {
    private var engine: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(port: Int = 8080) {
        if (engine != null) {
            Log.d(TAG, "SyncServer already running")
            return
        }

        try {
            engine = embeddedServer(CIO, port = port) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }

                routing {
                    // Ping/Info endpoint
                    get("/info") {
                        val activeTokos = tokoDao.getAllActiveTokos().map {
                            TokoSummaryDto(
                                id = it.id,
                                namaToko = it.namaToko,
                                ownerName = it.ownerDeviceName
                            )
                        }
                        call.respond(
                            DeviceInfoDto(
                                deviceId = prefs.deviceId,
                                deviceName = prefs.deviceName.ifBlank { "Unknown" },
                                servedTokos = activeTokos
                            )
                        )
                    }

                    // Request to join a Toko
                    post("/toko/{tokoId}/join") {
                        val tokoId = call.parameters["tokoId"]
                        if (tokoId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing tokoId")
                            return@post
                        }

                        val toko = tokoDao.getTokoById(tokoId)
                        if (toko == null) {
                            call.respond(HttpStatusCode.NotFound, "Toko not found")
                            return@post
                        }

                        val request = call.receive<JoinRequestDto>()
                        val now = System.currentTimeMillis()

                        // Auto register as USER if not exists
                        val existingMember = tokoMemberDao.getMemberByDevice(tokoId, request.deviceId)
                        val memberEntity = if (existingMember == null) {
                            val newMember = TokoMemberEntity(
                                id = UUID.randomUUID().toString(),
                                tokoId = tokoId,
                                deviceId = request.deviceId,
                                deviceName = request.deviceName,
                                role = MemberRole.USER.name,
                                roleChangedAt = now,
                                joinedAt = now,
                                updatedAt = now,
                                isActive = true
                            )
                            tokoMemberDao.upsertMember(newMember)
                            newMember
                        } else {
                            existingMember
                        }

                        call.respond(
                            JoinResponseDto(
                                success = true,
                                message = "Berhasil bergabung ke toko ${toko.namaToko}",
                                toko = toko.toDto(),
                                member = memberEntity.toDto()
                            )
                        )
                    }

                    // Pull sync endpoint (download changes)
                    get("/sync/{tokoId}") {
                        val tokoId = call.parameters["tokoId"]
                        if (tokoId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing tokoId")
                            return@get
                        }
                        val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                        val payload = syncRepository.createSyncPayload(tokoId, since)
                        call.respond(payload)
                    }

                    // Push sync endpoint (upload changes)
                    post("/sync/{tokoId}") {
                        val tokoId = call.parameters["tokoId"]
                        if (tokoId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing tokoId")
                            return@post
                        }
                        val payload = call.receive<SyncPayloadDto>()
                        val result = syncRepository.applySyncPayload(payload)
                        result.fold(
                            onSuccess = { syncResult -> call.respond(HttpStatusCode.OK, syncResult) },
                            onFailure = { error ->
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    error.message ?: "Sync error"
                                )
                            }
                        )
                    }
                }
            }

            scope.launch {
                try {
                    engine?.start(wait = false)
                    Log.i(TAG, "SyncServer started on port $port")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start SyncServer", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SyncServer", e)
        }
    }

    fun stop() {
        try {
            engine?.stop(1000, 2000)
            engine = null
            Log.i(TAG, "SyncServer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SyncServer", e)
        }
    }

    companion object {
        private const val TAG = "SyncServer"
    }
}
