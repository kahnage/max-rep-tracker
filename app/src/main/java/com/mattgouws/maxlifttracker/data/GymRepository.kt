package com.mattgouws.maxlifttracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Single point of database access for the rest of the app. Web sync (#12)
 * will hook in here.
 */
class GymRepository(
    private val metricDao: MetricDao,
    private val entryDao: EntryDao,
    private val workoutDao: WorkoutDao,
) {
    fun getAllMetrics(): Flow<List<TrackedMetric>> = metricDao.getAllMetrics()

    fun getAllMetricsWithMax(): Flow<List<MetricWithMax>> = metricDao.getAllMetricsWithMax()

    fun getMetric(id: Long): Flow<TrackedMetric?> = metricDao.getMetricById(id)

    fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>> =
        entryDao.getEntriesForMetric(metricId)

    fun getMaxValueForMetric(metricId: Long): Flow<Double?> =
        entryDao.getMaxValueForMetric(metricId)

    suspend fun addMetric(name: String, unit: String = "kg"): Long =
        metricDao.insert(TrackedMetric(name = name, unit = unit))

    suspend fun logEntry(
        metricId: Long,
        value: Double,
        loggedAt: Long = System.currentTimeMillis(),
    ): Long = entryDao.insert(MetricEntry(metricId = metricId, value = value, loggedAt = loggedAt))

    suspend fun deleteMetric(id: Long) = metricDao.deleteById(id)

    suspend fun deleteEntry(id: Long) = entryDao.deleteById(id)

    /** All workouts, main ones in rotation order then adhoc ones by name, with exercises in order. */
    fun getAllWorkouts(): Flow<List<WorkoutWithExercises>> =
        workoutDao.getAllWorkoutsWithExercises().map { workouts ->
            workouts.map { it.copy(exercises = it.exercises.sortedBy { exercise -> exercise.position }) }
        }

    /** What's coming up, up next first. See [buildSchedule]. */
    fun getSchedule(): Flow<List<ScheduledWorkout>> = combine(
        getAllWorkouts(),
        workoutDao.getQueue(),
        workoutDao.getLastCompletedMainOrder(),
        ::buildSchedule,
    )

    fun getLatestCompletion(): Flow<WorkoutCompletion?> = workoutDao.getLatestCompletion()

    /** Creates or updates [workout] with [exercises] in the given order. Returns the workout's id. */
    suspend fun saveWorkout(workout: Workout, exercises: List<WorkoutExercise>): Long =
        workoutDao.saveWorkout(workout, exercises)

    suspend fun deleteWorkout(id: Long) = workoutDao.deleteWorkoutById(id)

    /** Queues an adhoc workout to be done once [mainWorkoutsBefore] more main workouts are done. */
    suspend fun queueWorkout(workoutId: Long, mainWorkoutsBefore: Int): Long =
        workoutDao.insertQueued(QueuedWorkout(workoutId = workoutId, mainWorkoutsBefore = mainWorkoutsBefore))

    suspend fun removeFromQueue(queueEntryId: Long) = workoutDao.deleteQueuedById(queueEntryId)

    suspend fun completeWorkout(
        scheduled: ScheduledWorkout,
        completedAt: Long = System.currentTimeMillis(),
    ) = workoutDao.completeWorkout(scheduled.workout.workout, scheduled.queueEntryId, completedAt, skipped = false)

    /** Moves past [scheduled] without doing it. It's recorded as skipped, not shown as last done. */
    suspend fun skipWorkout(
        scheduled: ScheduledWorkout,
        skippedAt: Long = System.currentTimeMillis(),
    ) = workoutDao.completeWorkout(scheduled.workout.workout, scheduled.queueEntryId, skippedAt, skipped = true)
}
