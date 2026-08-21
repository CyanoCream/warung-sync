package com.warungsync.app.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val tokoId: String,
    val namaKategori: String,
    val colorArgb: Int = -11581723,
    val updatedAt: Long,
    val updatedByDevice: String,
    val isDeleted: Boolean = false
)

@Serializable
data class ItemDto(
    val id: String,
    val tokoId: String,
    val namaBarang: String,
    val deskripsi: String? = null,
    val harga: Double,
    val satuan: String,
    val unitQuantity: Double = 1.0,
    val categoryId: String,
    val categoryIds: List<String> = emptyList(),
    val updatedAt: Long,
    val updatedByDevice: String,
    val isDeleted: Boolean = false
)

@Serializable
data class PriceHistoryDto(
    val id: String,
    val tokoId: String,
    val itemId: String,
    val hargaLama: Double,
    val hargaBaru: Double,
    val satuanLama: String,
    val satuanBaru: String,
    val changedAt: Long,
    val changedByDevice: String
)

@Serializable
data class TokoDto(
    val id: String,
    val namaToko: String,
    val ownerDeviceId: String,
    val ownerDeviceName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class TokoMemberDto(
    val id: String,
    val tokoId: String,
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val roleChangedAt: Long,
    val joinedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

@Serializable
data class SyncPayloadDto(
    val tokoId: String,
    val deviceId: String,
    val deviceName: String,
    val senderRole: String = "USER",
    val toko: TokoDto? = null,
    val members: List<TokoMemberDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val items: List<ItemDto> = emptyList(),
    val histories: List<PriceHistoryDto> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceInfoDto(
    val deviceId: String,
    val deviceName: String,
    val appVersion: String = "1.0.0",
    val servedTokos: List<TokoSummaryDto> = emptyList()
)

@Serializable
data class TokoSummaryDto(
    val id: String,
    val namaToko: String,
    val ownerName: String
)

@Serializable
data class JoinRequestDto(
    val deviceId: String,
    val deviceName: String
)

@Serializable
data class JoinResponseDto(
    val success: Boolean,
    val message: String,
    val toko: TokoDto? = null,
    val member: TokoMemberDto? = null
)
