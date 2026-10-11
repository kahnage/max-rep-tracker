package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A record of a workout being done. The workout's name, type and rotation order are copied in so
 * history, and the rotation's position, survive the workout being edited or deleted.
 */
@Entity(
    tableName = "workout_completions",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long?,
    val workoutName: String,
    val workoutType: WorkoutType,
    val rotationOrder: Int?,
    val completedAt: Long = System.currentTimeMillis(),
    // Groundwork for web sync (#12): unused for now.
    val remoteId: String? = null,
    val isSynced: Boolean = false,
)
