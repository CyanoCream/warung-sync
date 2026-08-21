package com.warungsync.app.domain.model

data class TokoMember(
    val id: String,
    val tokoId: String,
    val deviceId: String,
    val deviceName: String,
    val role: MemberRole,
    val roleChangedAt: Long,
    val joinedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)
