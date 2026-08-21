package com.warungsync.app.domain.model

data class Toko(
    val id: String,
    val namaToko: String,
    val ownerDeviceId: String,
    val ownerDeviceName: String,
    val myRole: MemberRole,
    val memberCount: Int = 1,
    val createdAt: Long,
    val updatedAt: Long
)
