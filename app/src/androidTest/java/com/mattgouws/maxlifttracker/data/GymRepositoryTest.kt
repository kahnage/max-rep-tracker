package com.mattgouws.maxlifttracker.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: GymRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = GymRepository(db.metricDao(), db.entryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addMetricAndLogEntries() = runTest {
        val squatId = repository.addMetric("Squat")
        repository.addMetric("Bench", unit = "lb")
        repository.logEntry(squatId, 60.0, loggedAt = 1_000)
        repository.logEntry(squatId, 75.0, loggedAt = 2_000)

        assertEquals("kg", repository.getMetric(squatId).first()?.unit)
        assertEquals(listOf(75.0, 60.0), repository.getEntriesForMetric(squatId).first().map { it.value })
        assertEquals(75.0, repository.getMaxValueForMetric(squatId).first()!!, 0.0)

        val withMax = repository.getAllMetricsWithMax().first()
        assertEquals(listOf("Bench", "Squat"), withMax.map { it.metric.name })
        assertNull(withMax[0].maxValue)
        assertEquals(75.0, withMax[1].maxValue!!, 0.0)
    }

    /** Minimal ViewModel shaped like the real screens will be (#5 onwards). */
    private class TestViewModel(private val repository: GymRepository) : ViewModel() {
        val metrics: StateFlow<List<MetricWithMax>> = repository.getAllMetricsWithMax()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        fun addMetric(name: String) {
            viewModelScope.launch { repository.addMetric(name) }
        }
    }

    @Test
    fun viewModelReadsAndWritesThroughRepository() {
        // Its own database, never closed: the ViewModel keeps observing it after the test ends, and
        // a read against the shared one closed in tearDown crashes the test process. Being in-memory,
        // it's discarded anyway.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val openDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val openRepository = GymRepository(openDb.metricDao(), openDb.entryDao())

        // viewModelScope runs on Dispatchers.Main, so create and drive the ViewModel on the main thread.
        lateinit var viewModel: TestViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = TestViewModel(openRepository)
            viewModel.addMetric("Deadlift")
        }

        val metrics = runBlocking {
            withTimeout(5_000) { viewModel.metrics.first { it.isNotEmpty() } }
        }
        assertEquals(listOf("Deadlift"), metrics.map { it.metric.name })
    }
}
