package com.mattgouws.maxlifttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {
    @Insert
    suspend fun insert(metric: TrackedMetric): Long

    @Query("SELECT * FROM tracked_metrics ORDER BY name COLLATE NOCASE")
    fun getAllMetrics(): Flow<List<TrackedMetric>>

    @Query("SELECT * FROM tracked_metrics WHERE id = :id")
    fun getMetricById(id: Long): Flow<TrackedMetric?>

    @Query(
        """
        SELECT tracked_metrics.*, MAX(metric_entries.value) AS maxValue
        FROM tracked_metrics
        LEFT JOIN metric_entries ON metric_entries.metricId = tracked_metrics.id
        GROUP BY tracked_metrics.id
        ORDER BY tracked_metrics.name COLLATE NOCASE
        """
    )
    fun getAllMetricsWithMax(): Flow<List<MetricWithMax>>
}
