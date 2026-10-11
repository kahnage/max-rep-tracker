package com.mattgouws.maxlifttracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: GymRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = GymRepository(db.metricDao(), db.entryDao(), db.workoutDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun addMain(name: String, order: Int) =
        repository.saveWorkout(Workout(name = name, type = WorkoutType.MAIN, rotationOrder = order), emptyList())

    private suspend fun addAdhoc(name: String) =
        repository.saveWorkout(Workout(name = name, type = WorkoutType.ADHOC), emptyList())

    private suspend fun upNext() = repository.getSchedule().first().first().workout.workout.name

    private suspend fun completeUpNext(at: Long) = repository.completeWorkout(repository.getSchedule().first().first(), at)

    @Test
    fun rotationCyclesAndQueuedAdhocWorkoutsSlotIn() = runTest {
        addMain("Workout 1", 1)
        addMain("Workout 2", 2)
        addMain("Workout 3", 3)
        val cardioId = addAdhoc("Cardio")
        assertEquals("Workout 1", upNext())

        completeUpNext(at = 1_000)
        assertEquals("Workout 2", upNext())

        // Queued in 2 workouts' time: Workout 2 first, then Cardio.
        repository.queueWorkout(cardioId, mainWorkoutsBefore = 1)
        assertEquals(
            listOf("Workout 2", "Cardio", "Workout 3", "Workout 1"),
            repository.getSchedule().first().map { it.workout.workout.name },
        )

        completeUpNext(at = 2_000)
        assertEquals("Cardio", upNext())

        // Doing the adhoc workout doesn't move the rotation on.
        completeUpNext(at = 3_000)
        assertEquals("Workout 3", upNext())
        assertEquals("Cardio", repository.getLatestCompletion().first()?.workoutName)

        completeUpNext(at = 4_000)
        assertEquals("Workout 1", upNext())
        assertEquals(emptyList<QueuedWorkout>(), db.workoutDao().getQueue().first())
    }

    @Test
    fun queueNextIsDoneBeforeTheRotationContinues() = runTest {
        addMain("Workout 1", 1)
        addMain("Workout 2", 2)
        val cardioId = addAdhoc("Cardio")
        completeUpNext(at = 1_000)

        repository.queueWorkout(cardioId, mainWorkoutsBefore = 0)
        assertEquals("Cardio", upNext())

        // Removing it from the queue puts the rotation back.
        val entry = repository.getSchedule().first().first()
        repository.removeFromQueue(entry.queueEntryId!!)
        assertEquals("Workout 2", upNext())
    }

    @Test
    fun savingReplacesExercisesInTheGivenOrder() = runTest {
        val id = repository.saveWorkout(
            Workout(name = "Push", type = WorkoutType.MAIN, rotationOrder = 1),
            listOf(WorkoutExercise(name = "Bench", sets = 4, reps = 8), WorkoutExercise(name = "Dips", sets = 3, reps = 12)),
        )
        val saved = repository.getAllWorkouts().first().single()
        assertEquals(listOf("Bench", "Dips"), saved.exercises.map { it.name })

        repository.saveWorkout(
            saved.workout.copy(name = "Push A"),
            listOf(
                WorkoutExercise(name = "Dips", sets = 3, reps = 12),
                WorkoutExercise(name = "Bench", sets = 5, reps = 5),
                WorkoutExercise(name = "Flyes", sets = 3, reps = 15),
            ),
        )
        val updated = repository.getAllWorkouts().first().single()
        assertEquals(id, updated.workout.id)
        assertEquals("Push A", updated.workout.name)
        assertEquals(listOf("Dips", "Bench", "Flyes"), updated.exercises.map { it.name })
        assertEquals(listOf(0, 1, 2), updated.exercises.map { it.position })
        assertEquals(5, updated.exercises[1].sets)
    }

    @Test
    fun changingAQueuedAdhocWorkoutToMainRemovesItFromTheQueue() = runTest {
        addMain("Workout 1", 1)
        val cardioId = addAdhoc("Cardio")
        repository.queueWorkout(cardioId, mainWorkoutsBefore = 0)
        assertEquals("Cardio", upNext())

        val cardio = repository.getAllWorkouts().first().first { it.workout.id == cardioId }.workout
        repository.saveWorkout(cardio.copy(type = WorkoutType.MAIN, rotationOrder = 2), emptyList())

        assertEquals(emptyList<QueuedWorkout>(), db.workoutDao().getQueue().first())
        assertEquals("Workout 1", upNext())
    }

    @Test
    fun deletingAWorkoutKeepsHistoryAndTheRotationPosition() = runTest {
        addMain("Workout 1", 1)
        val twoId = addMain("Workout 2", 2)
        addMain("Workout 3", 3)
        completeUpNext(at = 1_000)
        completeUpNext(at = 2_000)

        repository.deleteWorkout(twoId)

        assertEquals("Workout 3", upNext())
        val latest = repository.getLatestCompletion().first()
        assertEquals("Workout 2", latest?.workoutName)
        assertNull(latest?.workoutId)
    }

    @Test
    fun deletingAQueuedWorkoutRemovesItFromTheQueue() = runTest {
        addMain("Workout 1", 1)
        val cardioId = addAdhoc("Cardio")
        repository.queueWorkout(cardioId, mainWorkoutsBefore = 0)

        repository.deleteWorkout(cardioId)

        assertEquals(emptyList<QueuedWorkout>(), db.workoutDao().getQueue().first())
        assertEquals("Workout 1", upNext())
    }
}
