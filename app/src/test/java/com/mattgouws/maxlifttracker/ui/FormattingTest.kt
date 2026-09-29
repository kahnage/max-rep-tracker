package com.mattgouws.maxlifttracker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {
    @Test
    fun wholeNumbersHaveNoDecimal() {
        assertEquals("60kg", formatValue(60.0, "kg"))
        assertEquals("100lb", formatValue(100.0, "lb"))
    }

    @Test
    fun fractionsKeepTheirDecimals() {
        assertEquals("62.5kg", formatValue(62.5, "kg"))
        assertEquals("2.25kg", formatValue(2.25, "kg"))
    }
}
