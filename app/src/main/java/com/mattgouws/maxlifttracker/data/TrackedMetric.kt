package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_metrics")
data class TrackedMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String = "kg",
    val createdAt: Long = System.currentTimeMillis(),
    // Groundwork for web sync (#12): unused for now.
    val remoteId: String? = null,
    val isSynced: Boolean = false,
)
