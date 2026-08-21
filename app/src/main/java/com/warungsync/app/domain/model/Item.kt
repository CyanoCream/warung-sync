package com.warungsync.app.domain.model

data class Item(
    val id: String,
    val tokoId: String,
    val namaBarang: String,
    val deskripsi: String? = null,
    val harga: Double,
    val satuan: String,
    val unitQuantity: Double = 1.0,
    val categoryId: String,
    val categoryName: String? = null,
    val categoryColorArgb: Int = DEFAULT_CATEGORY_COLOR_ARGB,
    val categoryIds: List<String> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val categoryColorsArgb: List<Int> = emptyList(),
    val updatedAt: Long,
    val updatedByDevice: String
)
