package com.mattgouws.maxlifttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY rotationOrder, name COLLATE NOCASE")
    abstract fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>

    @Query("SELECT * FROM workout_queue ORDER BY id")
    abstract fun getQueue(): Flow<List<QueuedWorkout>>

    /** The last workout actually done, ignoring skips. */
    @Query("SELECT * FROM workout_completions WHERE skipped = 0 ORDER BY completedAt DESC, id DESC LIMIT 1")
    abstract fun getLatestCompletion(): Flow<WorkoutCompletion?>

    // Skips count: skipping a main workout moves the rotation on just like doing it.
    @Query(
        """
        SELECT rotationOrder FROM workout_completions WHERE workoutType = 'MAIN'
        ORDER BY completedAt DESC, id DESC LIMIT 1
        """
    )
    abstract fun getLastCompletedMainOrder(): Flow<Int?>

    @Insert
    abstract suspend fun insertWorkout(workout: Workout): Long

    @Update
    abstract suspend fun updateWorkout(workout: Workout)

    // Exercises and queue entries are removed with it by ON DELETE CASCADE; completions are kept.
    @Query("DELETE FROM workouts WHERE id = :id")
    abstract suspend fun deleteWorkoutById(id: Long)

    @Insert
    abstract suspend fun insertExercises(exercises: List<WorkoutExercise>)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    abstract suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Insert
    abstract suspend fun insertQueued(entry: QueuedWorkout): Long

    @Query("DELETE FROM workout_queue WHERE id = :id")
    abstract suspend fun deleteQueuedById(id: Long)

    @Query("DELETE FROM workout_queue WHERE workoutId = :workoutId")
    abstract suspend fun deleteQueuedForWorkout(workoutId: Long)

    @Query("UPDATE workout_queue SET mainWorkoutsBefore = mainWorkoutsBefore - 1 WHERE mainWorkoutsBefore > 0")
    abstract suspend fun countDownQueue()

    @Insert
    abstract suspend fun insertCompletion(completion: WorkoutCompletion): Long

    /** Inserts or updates [workout] and replaces its exercises, in the given order. Returns the workout's id. */
    @Transaction
    open suspend fun saveWorkout(workout: Workout, exercises: List<WorkoutExercise>): Long {
        val id = if (workout.id == 0L) {
            insertWorkout(workout)
        } else {
            updateWorkout(workout)
            deleteExercisesForWorkout(workout.id)
            workout.id
        }
        // Only adhoc workouts can be queued.
        if (workout.type == WorkoutType.MAIN) deleteQueuedForWorkout(id)
        insertExercises(exercises.mapIndexed { index, it -> it.copy(id = 0, workoutId = id, position = index) })
        return id
    }

    /**
     * Records [workout] as done, or as [skipped]. Either way a main workout counts down the queue
     * and moves the rotation on, and a queued adhoc one is removed from the queue by its [queueEntryId].
     */
    @Transaction
    open suspend fun completeWorkout(workout: Workout, queueEntryId: Long?, completedAt: Long, skipped: Boolean) {
        insertCompletion(
            WorkoutCompletion(
                workoutId = workout.id,
                workoutName = workout.name,
                workoutType = workout.type,
                rotationOrder = workout.rotationOrder,
                completedAt = completedAt,
                skipped = skipped,
            ),
        )
        if (workout.type == WorkoutType.MAIN) countDownQueue()
        if (queueEntryId != null) deleteQueuedById(queueEntryId)
    }
}
