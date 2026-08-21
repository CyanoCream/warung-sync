package com.warungsync.app.domain.model

data class Item(
    val id: String,
    val tokoId: String,
    val namaBarang: String,
    val deskripsi: String? = null,
    val harga: Double,
    val satuan: String,
    val categoryId: String,
    val categoryName: String? = null,
    val updatedAt: Long,
    val updatedByDevice: String
)
