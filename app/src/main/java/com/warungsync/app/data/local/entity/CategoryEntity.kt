package com.warungsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = TokoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tokoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tokoId", "namaKategori"], unique = true),
        Index(value = ["tokoId"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val tokoId: String,
    val namaKategori: String,
    val updatedAt: Long,
    val updatedByDevice: String,
    val isDeleted: Boolean = false
)
