package com.mattgouws.maxlifttracker.ui.detail

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mattgouws.maxlifttracker.data.MetricEntry
import com.mattgouws.maxlifttracker.data.TrackedMetric
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressChartTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val day = 24 * 60 * 60 * 1000L

    // Newest first, as the DAO returns them.
    private fun entries(vararg values: Double) = values.mapIndexed { i, v ->
        MetricEntry(id = i.toLong() + 1, metricId = 1, value = v, loggedAt = 1_788_000_000_000 + (values.size - i) * day)
    }

    private fun showDetail(entries: List<MetricEntry>) {
        composeRule.setContent {
            DetailContent(
                DetailUiState(metric = TrackedMetric(id = 1, name = "Squat"), entries = entries, isLoading = false),
                onBack = {},
                onLogEntry = {},
            )
        }
    }

    @Test
    fun hiddenWithNoEntries() {
        showDetail(emptyList())
        composeRule.onNodeWithTag("progressChart").assertDoesNotExist()
    }

    @Test
    fun hiddenWithOneEntry() {
        showDetail(entries(100.0))
        composeRule.onNodeWithTag("progressChart").assertDoesNotExist()
    }

    @Test
    fun shownWithTwoEntries() {
        showDetail(entries(110.0, 100.0))
        composeRule.onNodeWithTag("progressChart")
            .assertIsDisplayed()
            .assert(hasDescription("Progress chart of 2 entries, from 100kg to 110kg. Best 110kg."))
    }

    @Test
    fun shownWithManyEntries() {
        showDetail(entries(*DoubleArray(40) { 60.0 + it }.reversedArray()))
        composeRule.onNodeWithTag("progressChart")
            .assertIsDisplayed()
            .assert(hasDescription("Progress chart of 40 entries, from 60kg to 99kg. Best 99kg."))
    }

    @Test
    fun handlesEntriesLoggedAtTheSameMoment() {
        val sameTime = listOf(
            MetricEntry(id = 1, metricId = 1, value = 100.0, loggedAt = 1_000),
            MetricEntry(id = 2, metricId = 1, value = 105.0, loggedAt = 1_000),
        )
        showDetail(sameTime)
        composeRule.onNodeWithTag("progressChart").assertIsDisplayed()
    }

    @Test
    fun tappingSelectsNearestPointAndTappingAgainClears() {
        showDetail(entries(120.0, 110.0, 100.0))
        val chart = composeRule.onNodeWithTag("progressChart")

        // The right edge is the most recent entry.
        chart.performTouchInput { click(centerRight.copy(x = right - 20f)) }
        chart.assert(SemanticsMatcher("selected 120kg") {
            it.config.getOrNull(SemanticsProperties.StateDescription)?.startsWith("120kg") == true
        })

        chart.performTouchInput { click(centerRight.copy(x = right - 20f)) }
        chart.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    private fun hasDescription(text: String) = SemanticsMatcher("contentDescription = $text") {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text) == true
    }
}
