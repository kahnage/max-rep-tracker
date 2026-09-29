package com.mattgouws.maxlifttracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var metricDao: MetricDao
    private lateinit var entryDao: EntryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        metricDao = db.metricDao()
        entryDao = db.entryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadMetrics() = runTest {
        val squatId = metricDao.insert(TrackedMetric(name = "Squat"))
        metricDao.insert(TrackedMetric(name = "bench", unit = "lb"))

        val all = metricDao.getAllMetrics().first()
        assertEquals(listOf("bench", "Squat"), all.map { it.name })

        val squat = metricDao.getMetricById(squatId).first()
        assertEquals("Squat", squat?.name)
        assertEquals("kg", squat?.unit)
        assertEquals(false, squat?.isSynced)
        assertNull(metricDao.getMetricById(999).first())
    }

    @Test
    fun entriesAreScopedToMetricAndNewestFirst() = runTest {
        val squatId = metricDao.insert(TrackedMetric(name = "Squat"))
        val benchId = metricDao.insert(TrackedMetric(name = "Bench"))
        entryDao.insert(MetricEntry(metricId = squatId, value = 60.0, loggedAt = 1_000))
        entryDao.insert(MetricEntry(metricId = squatId, value = 70.0, loggedAt = 3_000))
        entryDao.insert(MetricEntry(metricId = squatId, value = 65.0, loggedAt = 2_000))
        entryDao.insert(MetricEntry(metricId = benchId, value = 50.0, loggedAt = 4_000))

        val entries = entryDao.getEntriesForMetric(squatId).first()
        assertEquals(listOf(3_000L, 2_000L, 1_000L), entries.map { it.loggedAt })
        assertEquals(70.0, entryDao.getMaxValueForMetric(squatId).first()!!, 0.0)
        assertNull(entryDao.getMaxValueForMetric(999).first())
    }

    @Test
    fun metricsWithMaxIncludesMetricsWithoutEntries() = runTest {
        val squatId = metricDao.insert(TrackedMetric(name = "Squat"))
        metricDao.insert(TrackedMetric(name = "Deadlift"))
        entryDao.insert(MetricEntry(metricId = squatId, value = 60.0))
        entryDao.insert(MetricEntry(metricId = squatId, value = 80.0))

        val result = metricDao.getAllMetricsWithMax().first()
        assertEquals(listOf("Deadlift", "Squat"), result.map { it.metric.name })
        assertNull(result[0].maxValue)
        assertEquals(80.0, result[1].maxValue!!, 0.0)
    }

    @Test
    fun deletingMetricCascadesToEntries() = runTest {
        val squatId = metricDao.insert(TrackedMetric(name = "Squat"))
        entryDao.insert(MetricEntry(metricId = squatId, value = 60.0))

        db.openHelper.writableDatabase.execSQL("DELETE FROM tracked_metrics WHERE id = $squatId")

        assertEquals(0, entryDao.getEntriesForMetric(squatId).first().size)
    }
}
