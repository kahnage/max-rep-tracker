package com.mattgouws.maxlifttracker.ui.workouts

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mattgouws.maxlifttracker.data.AppDatabase
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.Workout
import com.mattgouws.maxlifttracker.data.WorkoutExercise
import com.mattgouws.maxlifttracker.data.WorkoutType
import com.mattgouws.maxlifttracker.ui.AppNavHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun hasNode(matcher: SemanticsMatcher) =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String, substring: Boolean = false) =
        composeRule.waitUntil(5_000) { hasNode(hasText(text, substring = substring)) }

    private fun inDialog(matcher: SemanticsMatcher) = matcher and hasAnyAncestor(isDialog())

    /**
     * Scrolls the Workouts list to the bottom, where adhoc workouts are. Scrolling only until a row
     * shows would leave its trailing icon under the FAB.
     */
    private fun scrollToBottom() {
        composeRule.onNode(hasScrollToKeyAction()).performScrollToKey("fab_spacer")
    }

    private fun queueCardio(option: String) {
        scrollToBottom()
        // Not performClick(): the previous action's snackbar can be covering the button.
        composeRule.onNodeWithContentDescription("Queue").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText(option).performClick()
        composeRule.onNode(hasScrollToKeyAction()).performScrollToKey("up_next")
    }

    private fun inMemoryRepository(): GymRepository {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Not closed: screens keep observing it until the compose rule tears down, after this test body.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        return GymRepository(db.metricDao(), db.entryDao(), db.workoutDao())
    }

    @Test
    fun creatingAWorkoutWithExercisesShowsItUpNext() {
        val repository = inMemoryRepository()
        composeRule.setContent { AppNavHost(repository) }
        waitForText("No workouts yet", substring = true)

        composeRule.onNodeWithContentDescription("Add workout").performClick()
        waitForText("New workout")
        composeRule.onNodeWithText("Name").performTextInput("Push")
        // A first main workout defaults to order 1.
        composeRule.onNodeWithText("1").assertExists()

        composeRule.onNodeWithText("Add exercise").performClick()
        composeRule.onNode(inDialog(hasText("Name"))).performTextInput("Bench press")
        composeRule.onNode(inDialog(hasText("Sets"))).performTextReplacement("4")
        composeRule.onNode(inDialog(hasText("Reps"))).performTextReplacement("8")
        composeRule.onNode(inDialog(hasText("Save"))).performClick()

        composeRule.onNodeWithText("Add exercise").performClick()
        composeRule.onNode(inDialog(hasText("Name"))).performTextInput("Dips")
        composeRule.onNode(inDialog(hasText("Save"))).performClick()

        // Move Dips above Bench press. The first row's Move up is disabled.
        composeRule.onAllNodesWithContentDescription("Move up")[0].assertIsNotEnabled()
        composeRule.onAllNodesWithContentDescription("Move up")[1].performClick()

        composeRule.onNodeWithText("Save").performClick()
        waitForText("Saved Push")
        composeRule.onNodeWithText("Up next").assertExists()
        composeRule.onNodeWithText("4 × 8").assertExists()

        val saved = runBlocking { repository.getAllWorkouts().first() }.single()
        assertEquals(WorkoutType.MAIN, saved.workout.type)
        assertEquals(1, saved.workout.rotationOrder)
        assertEquals(listOf("Dips", "Bench press"), saved.exercises.map { it.name })
        assertEquals(listOf(3 to 10, 4 to 8), saved.exercises.map { it.sets to it.reps })
    }

    @Test
    fun editorBlocksDuplicateNamesAndOrders() {
        val repository = inMemoryRepository()
        runBlocking {
            repository.saveWorkout(Workout(name = "Push", type = WorkoutType.MAIN, rotationOrder = 1), emptyList())
        }
        composeRule.setContent { AppNavHost(repository) }
        waitForText("Up next")

        composeRule.onNodeWithContentDescription("Add workout").performClick()
        waitForText("New workout")
        composeRule.onNodeWithText("Name").performTextInput("push")
        waitForText("You already have a workout called push")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("push").performTextReplacement("Pull")
        composeRule.onNodeWithText("2").performTextReplacement("1")
        waitForText("Push already uses this order")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        // Adhoc workouts have no order, so the clash goes away.
        composeRule.onNodeWithText("Adhoc").performClick()
        composeRule.onNodeWithText("Push already uses this order").assertDoesNotExist()
        composeRule.onNodeWithText("Save").performClick()
        waitForText("Saved Pull")
    }

    @Test
    fun completingAndQueueingWorkouts() {
        val repository = inMemoryRepository()
        runBlocking {
            repository.saveWorkout(
                Workout(name = "Workout 1", type = WorkoutType.MAIN, rotationOrder = 1),
                listOf(WorkoutExercise(name = "Squat", sets = 5, reps = 5)),
            )
            repository.saveWorkout(Workout(name = "Workout 2", type = WorkoutType.MAIN, rotationOrder = 2), emptyList())
            repository.saveWorkout(Workout(name = "Cardio", type = WorkoutType.ADHOC), emptyList())
        }
        composeRule.setContent { AppNavHost(repository) }
        waitForText("Up next")

        fun assertUpNext(name: String) = composeRule.waitUntil(5_000) {
            runBlocking { repository.getSchedule().first() }.first().workout.workout.name == name &&
                hasNode(hasText("Mark complete"))
        }

        assertUpNext("Workout 1")
        composeRule.onNodeWithText("Mark complete").performClick()
        // Cancelling does nothing.
        composeRule.onNodeWithText("Cancel").performClick()
        assertUpNext("Workout 1")

        composeRule.onNodeWithText("Mark complete").performClick()
        composeRule.onNodeWithText("Complete").performClick()
        waitForText("Completed Workout 1")
        assertUpNext("Workout 2")
        waitForText("Last done: Workout 1", substring = true)

        // Queue the adhoc workout next.
        queueCardio("Queue next")
        waitForText("Cardio is up next")
        assertUpNext("Cardio")
        composeRule.onNodeWithText("Adhoc · queued").assertExists()

        composeRule.onNodeWithText("Mark complete").performClick()
        composeRule.onNodeWithText("Complete").performClick()
        waitForText("Completed Cardio")
        assertUpNext("Workout 2")

        // Queue it in 2 workouts' time: it lands after Workout 2.
        queueCardio("Queue in 2 workouts' time")
        waitForText("Cardio queued after the next workout")
        assertUpNext("Workout 2")

        composeRule.onNodeWithText("Mark complete").performClick()
        composeRule.onNodeWithText("Complete").performClick()
        assertUpNext("Cardio")

        // Skipping it takes it off the queue and goes back to the rotation, which has wrapped round to Workout 1.
        composeRule.onNodeWithText("Skip").performClick()
        waitForText("It's taken off the queue", substring = true)
        composeRule.onNode(inDialog(hasText("Skip"))).performClick()
        waitForText("Skipped Cardio")
        assertUpNext("Workout 1")
        assertEquals(0, runBlocking { repository.getSchedule().first() }.count { it.queueEntryId != null })

        // Skipping a main workout moves the rotation on without changing "Last done".
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.onNodeWithText("Skip Workout 1?").assertExists()
        composeRule.onNode(inDialog(hasText("Skip"))).performClick()
        waitForText("Skipped Workout 1")
        assertUpNext("Workout 2")
        waitForText("Last done: Workout 2", substring = true)

        // Cancelling a skip does nothing.
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertUpNext("Workout 2")
    }

    @Test
    fun longPressDeletesAWorkout() {
        val repository = inMemoryRepository()
        runBlocking {
            repository.saveWorkout(Workout(name = "Workout 1", type = WorkoutType.MAIN, rotationOrder = 1), emptyList())
            repository.saveWorkout(Workout(name = "Cardio", type = WorkoutType.ADHOC), emptyList())
        }
        composeRule.setContent { AppNavHost(repository) }
        waitForText("Cardio")
        scrollToBottom()

        composeRule.onNodeWithText("Cardio").performTouchInput { longClick() }
        composeRule.onNodeWithText("Delete Cardio?").assertExists()
        composeRule.onNodeWithText("Delete").performClick()
        waitForText("Deleted Cardio")
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText("Cardio").fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf("Workout 1"), runBlocking { repository.getAllWorkouts().first() }.map { it.workout.name })
    }

    @Test
    fun tappingAWorkoutOpensItForEditing() {
        val repository = inMemoryRepository()
        runBlocking {
            repository.saveWorkout(
                Workout(name = "Cardio", type = WorkoutType.ADHOC),
                listOf(WorkoutExercise(name = "Rower", sets = 1, reps = 20)),
            )
        }
        composeRule.setContent { AppNavHost(repository) }
        waitForText("Cardio")

        composeRule.onNodeWithText("Cardio").performClick()
        waitForText("Edit workout")
        composeRule.onNodeWithText("Rower").performClick()
        composeRule.onNode(inDialog(hasText("20"))).performTextReplacement("30")
        composeRule.onNode(inDialog(hasText("Save"))).performClick()
        composeRule.onNodeWithText("1 × 30").assertExists()

        composeRule.onNodeWithText("Save").performClick()
        waitForText("Saved Cardio")
        val saved = runBlocking { repository.getAllWorkouts().first() }.single()
        assertEquals(30, saved.exercises.single().reps)
    }
}
