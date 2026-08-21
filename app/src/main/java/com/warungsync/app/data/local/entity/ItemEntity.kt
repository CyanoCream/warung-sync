package com.warungsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = TokoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tokoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["tokoId"]),
        Index(value = ["categoryId"]),
        Index(value = ["namaBarang"])
    ]
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val tokoId: String,
    val namaBarang: String,
    val deskripsi: String? = null,
    val harga: Double,
    val satuan: String,
    val unitQuantity: Double = 1.0,
    val categoryId: String,
    val updatedAt: Long,
    val updatedByDevice: String,
    val isDeleted: Boolean = false
)
