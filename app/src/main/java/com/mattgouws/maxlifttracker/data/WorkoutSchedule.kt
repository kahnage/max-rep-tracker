package com.mattgouws.maxlifttracker.data

/** A workout in the upcoming schedule. [queueEntryId] is set when it's a queued adhoc workout. */
data class ScheduledWorkout(
    val workout: WorkoutWithExercises,
    val queueEntryId: Long? = null,
)

/**
 * Works out what's coming up, the first entry being up next. Main workouts run in rotation order,
 * starting after the last one completed and wrapping back to the first. Queued adhoc workouts are
 * slotted in once their [QueuedWorkout.mainWorkoutsBefore] main workouts have been done.
 *
 * The result covers one full rotation, extended as far as needed to place every queued workout.
 */
fun buildSchedule(
    workouts: List<WorkoutWithExercises>,
    queue: List<QueuedWorkout>,
    lastCompletedMainOrder: Int?,
): List<ScheduledWorkout> {
    val rotation = workouts
        .filter { it.workout.type == WorkoutType.MAIN }
        .sortedWith(compareBy({ it.workout.rotationOrder ?: Int.MAX_VALUE }, { it.workout.id }))
    val byId = workouts.associateBy { it.workout.id }
    val pending = queue.sortedBy { it.id }.filter { it.workoutId in byId }.toMutableList()

    if (rotation.isEmpty()) {
        // Nothing to count down against, so queued workouts just run in the order they were queued.
        return pending.map { ScheduledWorkout(byId.getValue(it.workoutId), it.id) }
    }

    val start = if (lastCompletedMainOrder == null) {
        0
    } else {
        rotation.indexOfFirst { (it.workout.rotationOrder ?: Int.MAX_VALUE) > lastCompletedMainOrder }
            .coerceAtLeast(0)
    }
    val schedule = mutableListOf<ScheduledWorkout>()
    var mainsPlaced = 0
    while (true) {
        val due = pending.filter { it.mainWorkoutsBefore <= mainsPlaced }
        pending.removeAll(due)
        due.forEach { schedule += ScheduledWorkout(byId.getValue(it.workoutId), it.id) }
        if (pending.isEmpty() && mainsPlaced >= rotation.size) break
        schedule += ScheduledWorkout(rotation[(start + mainsPlaced) % rotation.size])
        mainsPlaced++
    }
    return schedule
}
