package com.mattgouws.maxlifttracker.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mattgouws.maxlifttracker.data.MetricEntry
import com.mattgouws.maxlifttracker.data.TrackedMetric
import com.mattgouws.maxlifttracker.ui.formatDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val squat = TrackedMetric(id = 1, name = "Squat")

    @Test
    fun showsMetricNameAndEntriesMostRecentFirst() {
        val newer = MetricEntry(id = 2, metricId = 1, value = 82.5, loggedAt = 1_760_000_000_000)
        val older = MetricEntry(id = 1, metricId = 1, value = 80.0, loggedAt = 1_750_000_000_000)
        composeRule.setContent {
            DetailContent(
                DetailUiState(metric = squat, entries = listOf(newer, older), isLoading = false),
                onBack = {},
                onLogEntry = {},
            )
        }

        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithText(formatDateTime(newer.loggedAt)).assertIsDisplayed()
        composeRule.onNodeWithText(formatDateTime(older.loggedAt)).assertIsDisplayed()

        val newerTop = composeRule.onNodeWithText("82.5kg").fetchSemanticsNode().boundsInRoot.top
        val olderTop = composeRule.onNodeWithText("80kg").fetchSemanticsNode().boundsInRoot.top
        assertTrue("most recent entry should be listed first", newerTop < olderTop)
    }

    @Test
    fun showsEmptyStateWithNoEntries() {
        composeRule.setContent {
            DetailContent(DetailUiState(metric = squat, isLoading = false), onBack = {}, onLogEntry = {})
        }
        composeRule.onNodeWithText("No entries yet", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Log entry").assertIsDisplayed()
    }

    @Test
    fun backAndLogEntryCallTheirCallbacks() {
        var backs = 0
        var logs = 0
        composeRule.setContent {
            DetailContent(
                DetailUiState(metric = squat, isLoading = false),
                onBack = { backs++ },
                onLogEntry = { logs++ },
            )
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Log entry").performClick()
        assertEquals(1, backs)
        assertEquals(1, logs)
    }

    @Test
    fun showsNotFoundForMissingMetric() {
        composeRule.setContent {
            DetailContent(DetailUiState(metric = null, isLoading = false), onBack = {}, onLogEntry = {})
        }
        assertEquals(1, composeRule.onAllNodesWithText("no longer exists", substring = true).fetchSemanticsNodes().size)
    }
}
