package com.warungsync.app.domain.model

data class Category(
    val id: String,
    val tokoId: String,
    val namaKategori: String,
    val updatedAt: Long,
    val updatedByDevice: String
)
