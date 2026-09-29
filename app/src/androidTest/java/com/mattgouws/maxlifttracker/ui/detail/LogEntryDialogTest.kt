package com.mattgouws.maxlifttracker.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogEntryDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed: Double? = null
    private var dismissed = false

    private fun showDialog(unit: String = "kg") {
        composeRule.setContent {
            LogEntryDialog(unit = unit, onConfirm = { confirmed = it }, onDismiss = { dismissed = true })
        }
    }

    @Test
    fun saveIsDisabledUntilValidNumberEntered() {
        showDialog()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("Value (kg)").performTextInput("abc")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("Enter a number greater than 0").assertIsDisplayed()

        composeRule.onNodeWithText("Value (kg)").performTextReplacement("0")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("Value (kg)").performTextReplacement("82.5")
        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Enter a number greater than 0").assertDoesNotExist()
    }

    @Test
    fun savePassesParsedValue() {
        showDialog()
        composeRule.onNodeWithText("Value (kg)").performTextInput("82,5")
        composeRule.onNodeWithText("Save").performClick()
        assertEquals(82.5, confirmed!!, 0.0)
    }

    @Test
    fun showsMetricUnit() {
        showDialog(unit = "lb")
        composeRule.onNodeWithText("Value (lb)").assertIsDisplayed()
    }

    @Test
    fun cancelDismissesWithoutSaving() {
        showDialog()
        composeRule.onNodeWithText("Value (kg)").performTextInput("100")
        composeRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
        assertNull(confirmed)
    }
}
