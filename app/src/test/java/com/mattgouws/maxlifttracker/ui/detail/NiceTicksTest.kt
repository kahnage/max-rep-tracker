package com.mattgouws.maxlifttracker.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NiceTicksTest {
    private fun assertCovers(ticks: List<Double>, min: Double, max: Double) {
        assertTrue("$ticks should start at or below $min", ticks.first() <= min)
        assertTrue("$ticks should end at or above $max", ticks.last() >= max)
    }

    @Test
    fun roundStepsForTypicalLifts() {
        assertEquals(listOf(60.0, 65.0, 70.0, 75.0), niceTicks(60.0, 72.5))
        assertEquals(listOf(100.0, 120.0, 140.0, 160.0), niceTicks(100.0, 160.0))
    }

    @Test
    fun smallRangesUseFractionalSteps() {
        val ticks = niceTicks(20.0, 21.25)
        assertCovers(ticks, 20.0, 21.25)
        assertEquals(listOf(20.0, 20.5, 21.0, 21.5), ticks)
    }

    @Test
    fun identicalValuesGetPaddedRange() {
        val ticks = niceTicks(100.0, 100.0)
        assertCovers(ticks, 100.0, 100.0)
        assertTrue(ticks.first() < 100.0 && ticks.last() > 100.0)
    }

    @Test
    fun neverGoesBelowZero() {
        assertTrue(niceTicks(0.5, 0.5).first() >= 0.0)
        assertTrue(niceTicks(1.0, 9.0).first() >= 0.0)
    }

    @Test
    fun ticksAreEvenlySpacedAndClean() {
        val ticks = niceTicks(0.1, 0.35)
        val steps = ticks.zipWithNext { a, b -> b - a }
        steps.forEach { assertEquals(steps.first(), it, 1e-9) }
        ticks.forEach { assertEquals(it, Math.round(it * 1e6) / 1e6, 0.0) }
    }
}
