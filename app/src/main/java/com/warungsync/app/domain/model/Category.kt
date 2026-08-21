package com.warungsync.app.domain.model

data class Category(
    val id: String,
    val tokoId: String,
    val namaKategori: String,
    val colorArgb: Int = DEFAULT_CATEGORY_COLOR_ARGB,
    val updatedAt: Long,
    val updatedByDevice: String
)

const val DEFAULT_CATEGORY_COLOR_ARGB: Int = -11581723
