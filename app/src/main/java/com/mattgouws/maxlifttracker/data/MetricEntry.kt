package com.mattgouws.maxlifttracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metric_entries",
    foreignKeys = [
        ForeignKey(
            entity = TrackedMetric::class,
            parentColumns = ["id"],
            childColumns = ["metricId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("metricId")],
)
data class MetricEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metricId: Long,
    val value: Double,
    val loggedAt: Long = System.currentTimeMillis(),
    // Groundwork for web sync (#12): unused for now.
    val remoteId: String? = null,
    val isSynced: Boolean = false,
)
