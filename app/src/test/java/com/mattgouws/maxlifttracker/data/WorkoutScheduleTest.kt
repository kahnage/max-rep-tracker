package com.mattgouws.maxlifttracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutScheduleTest {
    private fun main(id: Long, order: Int) =
        WorkoutWithExercises(Workout(id = id, name = "W$order", type = WorkoutType.MAIN, rotationOrder = order), emptyList())

    private fun adhoc(id: Long, name: String) =
        WorkoutWithExercises(Workout(id = id, name = name, type = WorkoutType.ADHOC), emptyList())

    // Deliberately out of order, to show the rotation follows rotationOrder rather than list order.
    private val workouts = listOf(main(3, 3), main(1, 1), main(2, 2), adhoc(10, "Cardio"), adhoc(11, "Mobility"))

    private fun names(schedule: List<ScheduledWorkout>) = schedule.map { it.workout.workout.name }

    @Test
    fun startsAtFirstMainWorkoutWhenNothingDone() {
        assertEquals(listOf("W1", "W2", "W3"), names(buildSchedule(workouts, emptyList(), lastCompletedMainOrder = null)))
    }

    @Test
    fun upNextFollowsLastCompletedAndWrapsAround() {
        assertEquals("W2", names(buildSchedule(workouts, emptyList(), 1)).first())
        assertEquals("W3", names(buildSchedule(workouts, emptyList(), 2)).first())
        // After the last one, back to the first.
        assertEquals(listOf("W1", "W2", "W3"), names(buildSchedule(workouts, emptyList(), 3)))
    }

    @Test
    fun handlesGapsInOrderAndDeletedLastWorkout() {
        val gappy = listOf(main(1, 10), main(2, 20), main(3, 30))
        // Order 15 was completed and has since been deleted or renumbered: carry on from the next one up.
        assertEquals("W20", names(buildSchedule(gappy, emptyList(), 15)).first())
        assertEquals("W10", names(buildSchedule(gappy, emptyList(), 99)).first())
    }

    @Test
    fun queuedNextComesBeforeTheNextMainWorkout() {
        val queue = listOf(QueuedWorkout(id = 1, workoutId = 10, mainWorkoutsBefore = 0))
        val schedule = buildSchedule(workouts, queue, lastCompletedMainOrder = 1)
        assertEquals(listOf("Cardio", "W2", "W3", "W1"), names(schedule))
        assertEquals(1L, schedule.first().queueEntryId)
        assertNull(schedule[1].queueEntryId)
    }

    @Test
    fun queuedInTwoComesAfterTheNextMainWorkout() {
        val queue = listOf(QueuedWorkout(id = 1, workoutId = 10, mainWorkoutsBefore = 1))
        assertEquals(listOf("W2", "Cardio", "W3", "W1"), names(buildSchedule(workouts, queue, 1)))
    }

    @Test
    fun severalQueuedWorkoutsKeepTheOrderTheyWereQueuedIn() {
        val queue = listOf(
            QueuedWorkout(id = 1, workoutId = 11, mainWorkoutsBefore = 1),
            QueuedWorkout(id = 2, workoutId = 10, mainWorkoutsBefore = 0),
            QueuedWorkout(id = 3, workoutId = 10, mainWorkoutsBefore = 1),
        )
        assertEquals(
            listOf("Cardio", "W2", "Mobility", "Cardio", "W3", "W1"),
            names(buildSchedule(workouts, queue, 1)),
        )
    }

    @Test
    fun extendsPastOneRotationToPlaceEveryQueuedWorkout() {
        val queue = listOf(QueuedWorkout(id = 1, workoutId = 10, mainWorkoutsBefore = 4))
        assertEquals(
            listOf("W1", "W2", "W3", "W1", "Cardio"),
            names(buildSchedule(workouts, queue, null)),
        )
    }

    @Test
    fun queuedWorkoutsStillRunWithNoMainWorkouts() {
        val adhocOnly = listOf(adhoc(10, "Cardio"), adhoc(11, "Mobility"))
        val queue = listOf(
            QueuedWorkout(id = 1, workoutId = 11, mainWorkoutsBefore = 1),
            QueuedWorkout(id = 2, workoutId = 10, mainWorkoutsBefore = 0),
        )
        assertEquals(listOf("Mobility", "Cardio"), names(buildSchedule(adhocOnly, queue, null)))
        assertEquals(emptyList<String>(), names(buildSchedule(adhocOnly, emptyList(), null)))
    }

    @Test
    fun ignoresQueueEntriesForMissingWorkouts() {
        val queue = listOf(QueuedWorkout(id = 1, workoutId = 999, mainWorkoutsBefore = 0))
        assertEquals(listOf("W1", "W2", "W3"), names(buildSchedule(workouts, queue, null)))
    }
}
