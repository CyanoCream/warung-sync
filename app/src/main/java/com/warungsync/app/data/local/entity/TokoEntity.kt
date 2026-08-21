package com.warungsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokos")
data class TokoEntity(
    @PrimaryKey
    val id: String,
    val namaToko: String,
    val ownerDeviceId: String,
    val ownerDeviceName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
