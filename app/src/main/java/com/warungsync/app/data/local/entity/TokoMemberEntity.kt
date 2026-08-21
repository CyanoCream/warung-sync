package com.warungsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "toko_members",
    foreignKeys = [
        ForeignKey(
            entity = TokoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tokoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tokoId", "deviceId"], unique = true),
        Index(value = ["tokoId"])
    ]
)
data class TokoMemberEntity(
    @PrimaryKey
    val id: String,
    val tokoId: String,
    val deviceId: String,
    val deviceName: String,
    val role: String, // "OWNER", "ADMIN", "USER"
    val roleChangedAt: Long,
    val joinedAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)
