package com.mattgouws.maxlifttracker.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mattgouws.maxlifttracker.data.AppDatabase
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.MetricWithMax
import com.mattgouws.maxlifttracker.data.TrackedMetric
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEmptyStateWithNoMetrics() {
        composeRule.setContent {
            HomeContent(HomeUiState(isLoading = false), onAddMetric = {}, onMetricClick = {})
        }
        composeRule.onNodeWithText("No lifts tracked yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsSingleMetricWithMax() {
        composeRule.setContent {
            HomeContent(
                HomeUiState(listOf(MetricWithMax(TrackedMetric(id = 1, name = "Squat"), 60.0)), isLoading = false),
                onAddMetric = {},
                onMetricClick = {},
            )
        }
        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithText("60kg").assertIsDisplayed()
    }

    @Test
    fun showsMultipleMetricsIncludingOneWithoutEntries() {
        composeRule.setContent {
            HomeContent(
                HomeUiState(
                    listOf(
                        MetricWithMax(TrackedMetric(id = 1, name = "Bench"), 62.5),
                        MetricWithMax(TrackedMetric(id = 2, name = "Deadlift", unit = "lb"), 225.0),
                        MetricWithMax(TrackedMetric(id = 3, name = "Squat"), null),
                    ),
                    isLoading = false,
                ),
                onAddMetric = {},
                onMetricClick = {},
            )
        }
        composeRule.onNodeWithText("62.5kg").assertIsDisplayed()
        composeRule.onNodeWithText("225lb").assertIsDisplayed()
        composeRule.onNodeWithText("No entries").assertIsDisplayed()
    }

    @Test
    fun addButtonCallsOnAddMetric() {
        var clicked = false
        composeRule.setContent {
            HomeContent(HomeUiState(isLoading = false), onAddMetric = { clicked = true }, onMetricClick = {})
        }
        composeRule.onNodeWithContentDescription("Add metric").performClick()
        assertTrue(clicked)
    }

    @Test
    fun updatesLiveWhenDataChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed: the screen keeps observing it until the compose rule tears down, after
        // this test body. Closing it here crashes that final read; being in-memory, it's discarded anyway.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = GymRepository(db.metricDao(), db.entryDao())
        val viewModel = HomeViewModel(repository)
        composeRule.setContent {
            HomeScreen(viewModel = viewModel, onMetricClick = {})
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No lifts tracked yet", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val squatId = runBlocking { repository.addMetric("Squat") }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No entries").fetchSemanticsNodes().isNotEmpty()
        }

        runBlocking { repository.logEntry(squatId, 80.0) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("80kg").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun addingMetricThroughDialogShowsItOnHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed, for the same reason as in updatesLiveWhenDataChanges.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = GymRepository(db.metricDao(), db.entryDao())
        val viewModel = HomeViewModel(repository)
        composeRule.setContent {
            HomeScreen(viewModel = viewModel, onMetricClick = {})
        }

        composeRule.onNodeWithContentDescription("Add metric").performClick()
        composeRule.onNodeWithText("Name").performTextInput("  Overhead Press  ")
        composeRule.onNodeWithText("lb").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Overhead Press").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        val saved = runBlocking { repository.getAllMetrics().first() }.single()
        assertEquals("Overhead Press", saved.name)
        assertEquals("lb", saved.unit)
    }
}
