package com.mattgouws.maxlifttracker.ui

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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun hasText(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun tappingMetricOpensDetailWithOnlyItsEntries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed: screens keep observing it until the compose rule tears down, after this test body.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = GymRepository(db.metricDao(), db.entryDao())
        runBlocking {
            val squatId = repository.addMetric("Squat")
            val benchId = repository.addMetric("Bench", unit = "lb")
            repository.logEntry(squatId, 100.0, loggedAt = 1_000)
            repository.logEntry(squatId, 110.0, loggedAt = 2_000)
            repository.logEntry(benchId, 185.0, loggedAt = 3_000)
        }

        composeRule.setContent { AppNavHost(repository) }

        composeRule.waitUntil(5_000) { hasText("Squat") }
        composeRule.onNodeWithText("Squat").performClick()

        // Detail shows only Squat's entries, not Bench's.
        composeRule.waitUntil(5_000) { hasText("100kg") }
        composeRule.onNodeWithText("110kg").assertExists()
        composeRule.onNodeWithText("185lb").assertDoesNotExist()
        composeRule.onNodeWithText("Bench").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(5_000) { hasText("Bench") }
        composeRule.onNodeWithText("Log entry").assertDoesNotExist()
    }

    private fun logEntry(value: String) {
        composeRule.onNodeWithText("Log entry").performClick()
        composeRule.onNodeWithText("Value (kg)").performTextInput(value)
        composeRule.onNodeWithText("Save").performClick()
    }

    @Test
    fun loggingEntryShowsInDetailAndUpdatesHomeMax() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed: screens keep observing it until the compose rule tears down, after this test body.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = GymRepository(db.metricDao(), db.entryDao())
        runBlocking {
            val squatId = repository.addMetric("Squat")
            repository.logEntry(squatId, 100.0, loggedAt = 1_000)
        }

        composeRule.setContent { AppNavHost(repository) }
        composeRule.waitUntil(5_000) { hasText("100kg") }
        composeRule.onNodeWithText("Squat").performClick()
        composeRule.waitUntil(5_000) { hasText("Log entry") }

        // A new best appears in Detail immediately, listed first.
        logEntry("120")
        composeRule.waitUntil(5_000) { hasText("120kg") }
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        val newTop = composeRule.onNodeWithText("120kg").fetchSemanticsNode().boundsInRoot.top
        val oldTop = composeRule.onNodeWithText("100kg").fetchSemanticsNode().boundsInRoot.top
        assertTrue("new entry should be listed first", newTop < oldTop)

        // A lower value is recorded but doesn't change the max.
        logEntry("90")
        composeRule.waitUntil(5_000) { hasText("90kg") }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(5_000) { hasText("120kg") }
        composeRule.onNodeWithText("100kg").assertDoesNotExist()
        composeRule.onNodeWithText("90kg").assertDoesNotExist()
    }
}
