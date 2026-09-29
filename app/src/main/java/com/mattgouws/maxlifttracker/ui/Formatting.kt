package com.mattgouws.maxlifttracker.ui

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formats a value with its unit without a trailing ".0", e.g. 60.0 -> "60kg", 62.5 -> "62.5kg". */
fun formatValue(value: Double, unit: String): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() + unit

/**
 * Parses a user-entered value, accepting "82.5" or "82,5". Returns null unless it's a finite
 * number greater than zero.
 */
fun parseValue(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }

/** Formats an epoch-millis timestamp as a medium date and short time in the device's locale and zone. */
fun formatDateTime(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
