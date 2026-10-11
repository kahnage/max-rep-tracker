package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An adhoc workout slotted in between main workouts. Entries fall due in [id] order. */
@Entity(
    tableName = "workout_queue",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class QueuedWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    // How many more main workouts must be completed before this one is due. 0 means it's due now.
    val mainWorkoutsBefore: Int,
)
