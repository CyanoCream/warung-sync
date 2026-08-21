package com.warungsync.app.presentation.util

/** Formats digits for display only: 4000 -> 4.000. */
fun formatThousandsInput(input: String): String {
    val digits = input.filter(Char::isDigit)
    if (digits.isEmpty()) return ""

    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return normalized
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

/** Converts a display-formatted Rupiah value back to its numeric value. */
fun parseThousandsInput(input: String): Double? =
    input.filter(Char::isDigit).takeIf(String::isNotEmpty)?.toDoubleOrNull()
