package com.mattgouws.maxlifttracker.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddMetricDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed: Pair<String, String>? = null
    private var dismissed = false

    private fun showDialog() {
        composeRule.setContent {
            AddMetricDialog(
                existingNames = listOf("Bench Press"),
                onConfirm = { name, unit -> confirmed = name to unit },
                onDismiss = { dismissed = true },
            )
        }
    }

    @Test
    fun saveIsDisabledUntilNameEntered() {
        showDialog()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("Name").performTextInput("   ")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("Name").performTextReplacement("Squat")
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun unitDefaultsToKg() {
        showDialog()
        composeRule.onNodeWithText("kg").assertIsSelected()
        composeRule.onNodeWithText("Name").performTextInput("Squat")
        composeRule.onNodeWithText("Save").performClick()
        assertEquals("Squat" to "kg", confirmed)
    }

    @Test
    fun canChooseLb() {
        showDialog()
        composeRule.onNodeWithText("Name").performTextInput("Deadlift")
        composeRule.onNodeWithText("lb").performClick()
        composeRule.onNodeWithText("Save").performClick()
        assertEquals("Deadlift" to "lb", confirmed)
    }

    @Test
    fun cancelDismissesWithoutSaving() {
        showDialog()
        composeRule.onNodeWithText("Name").performTextInput("Squat")
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
        assertEquals(null, confirmed)
    }

    @Test
    fun duplicateNameIsRejectedIgnoringCaseAndSpaces() {
        showDialog()
        composeRule.onNodeWithText("Name").performTextInput("  bench press ")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("already tracking", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("Name").performTextReplacement("Bench Press Incline")
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun nameIsCappedAtMaxLength() {
        showDialog()
        composeRule.onNodeWithText("Name").performTextInput("x".repeat(MAX_NAME_LENGTH + 10))
        composeRule.onNodeWithText("Save").performClick()
        assertEquals(MAX_NAME_LENGTH, confirmed!!.first.length)
    }
}
