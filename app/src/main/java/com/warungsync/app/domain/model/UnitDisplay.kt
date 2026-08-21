package com.warungsync.app.domain.model

import java.math.BigDecimal

fun formatUnitQuantity(quantity: Double, unit: String): String {
    val readableUnit = when (unit.trim().lowercase()) {
        "kg" -> "kilo"
        "pcs" -> "pcs"
        else -> unit.trim()
    }
    if (quantity == 1.0) return readableUnit

    val quantityText = BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString()
    return "$quantityText $readableUnit"
}
