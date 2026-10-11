package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
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
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long = 0,
    // Order within the workout, starting at 0.
    val position: Int = 0,
    val name: String,
    val sets: Int,
    val reps: Int,
)
