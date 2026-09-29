package com.mattgouws.maxlifttracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class FormattingTest {
    @Test
    fun wholeNumbersHaveNoDecimal() {
        assertEquals("60kg", formatValue(60.0, "kg"))
        assertEquals("100lb", formatValue(100.0, "lb"))
    }

    @Test
    fun dateTimeUsesGivenZoneAndLocale() {
        // 2026-09-29T20:30:00Z
        val millis = 1_790_713_800_000
        val text = formatDateTime(millis, ZoneId.of("Europe/London"), Locale.UK)
        assertTrue(text, text.contains("2026"))
        assertTrue(text, text.contains("21:30"))
    }

    @Test
    fun fractionsKeepTheirDecimals() {
        assertEquals("62.5kg", formatValue(62.5, "kg"))
        assertEquals("2.25kg", formatValue(2.25, "kg"))
    }
}
