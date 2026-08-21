package com.warungsync.app.domain.model

data class PricePoint(
    val timestamp: Long,
    val dateLabel: String,
    val price: Double
)

data class ItemTrendData(
    val item: Item,
    val initialPrice: Double,
    val currentPrice: Double,
    val priceChangeAmount: Double,
    val priceChangePercent: Double,
    val points: List<PricePoint>
)

enum class TrendTimeframe(val label: String, val monthsBack: Int) {
    THIS_MONTH("Bulan Ini", 1),
    LAST_3_MONTHS("3 Bulan", 3),
    LAST_6_MONTHS("6 Bulan", 6),
    FULL_YEAR("1 Tahun", 12),
    CUSTOM("Custom Tanggal", 0)
}

data class CustomDateRange(
    val startTimestamp: Long,
    val endTimestamp: Long
)
