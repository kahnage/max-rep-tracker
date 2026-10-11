package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: WorkoutType,
    // Position in the main rotation, lowest first. Null for adhoc workouts.
    val rotationOrder: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Groundwork for web sync (#12): unused for now.
    val remoteId: String? = null,
    val isSynced: Boolean = false,
)
