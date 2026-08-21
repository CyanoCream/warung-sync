package com.warungsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = TokoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tokoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tokoId"]),
        Index(value = ["itemId"]),
        Index(value = ["changedAt"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey
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
