package com.mattgouws.maxlifttracker.data

import androidx.room.Embedded

data class MetricWithMax(
    @Embedded val metric: TrackedMetric,
    // Null when the metric has no entries yet.
    val maxValue: Double?,
)
