package com.warungsync.app.domain.model

data class PriceHistory(
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
