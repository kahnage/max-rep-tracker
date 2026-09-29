package com.mattgouws.maxlifttracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: MetricEntry): Long

    @Query("SELECT * FROM metric_entries WHERE metricId = :metricId ORDER BY loggedAt DESC")
    fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>>

    @Query("SELECT MAX(value) FROM metric_entries WHERE metricId = :metricId")
    fun getMaxValueForMetric(metricId: Long): Flow<Double?>
}
