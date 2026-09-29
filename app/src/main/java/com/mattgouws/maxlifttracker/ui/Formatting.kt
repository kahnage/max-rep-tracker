package com.mattgouws.maxlifttracker.ui

import java.math.BigDecimal

/** Formats a value with its unit without a trailing ".0", e.g. 60.0 -> "60kg", 62.5 -> "62.5kg". */
fun formatValue(value: Double, unit: String): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() + unit
