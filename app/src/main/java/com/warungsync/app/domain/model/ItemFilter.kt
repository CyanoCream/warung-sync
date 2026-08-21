package com.warungsync.app.domain.model

enum class SortBy(val label: String) {
    DATE_DESC("Terbaru"),
    DATE_ASC("Terlama"),
    NAME_ASC("Nama A-Z"),
    NAME_DESC("Nama Z-A"),
    PRICE_ASC("Harga Terendah"),
    PRICE_DESC("Harga Tertinggi")
}

data class ItemFilter(
    val searchQuery: String = "",
    val categoryId: String? = null,
    val minHarga: Double? = null,
    val maxHarga: Double? = null,
    val sortBy: SortBy = SortBy.DATE_DESC
)
