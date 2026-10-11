package com.mattgouws.maxlifttracker.data

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    // Unordered as loaded; GymRepository sorts them by position.
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExercise>,
)
