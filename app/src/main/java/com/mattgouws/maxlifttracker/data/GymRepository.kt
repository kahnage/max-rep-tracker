package com.mattgouws.maxlifttracker.data

import kotlinx.coroutines.flow.Flow

/**
 * Single point of database access for the rest of the app. Web sync (#12)
 * will hook in here.
 */
class GymRepository(
    private val metricDao: MetricDao,
    private val entryDao: EntryDao,
) {
    fun getAllMetrics(): Flow<List<TrackedMetric>> = metricDao.getAllMetrics()

    fun getAllMetricsWithMax(): Flow<List<MetricWithMax>> = metricDao.getAllMetricsWithMax()

    fun getMetric(id: Long): Flow<TrackedMetric?> = metricDao.getMetricById(id)

    fun getEntriesForMetric(metricId: Long): Flow<List<MetricEntry>> =
        entryDao.getEntriesForMetric(metricId)

    fun getMaxValueForMetric(metricId: Long): Flow<Double?> =
        entryDao.getMaxValueForMetric(metricId)

    suspend fun addMetric(name: String, unit: String = "kg"): Long =
        metricDao.insert(TrackedMetric(name = name, unit = unit))

    suspend fun logEntry(
        metricId: Long,
        value: Double,
        loggedAt: Long = System.currentTimeMillis(),
    ): Long = entryDao.insert(MetricEntry(metricId = metricId, value = value, loggedAt = loggedAt))
}
