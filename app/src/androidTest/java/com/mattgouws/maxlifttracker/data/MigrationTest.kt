package com.mattgouws.maxlifttracker.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrating1To2KeepsExistingLiftsAndAddsWorkouts() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL("INSERT INTO tracked_metrics (id, name, unit, createdAt, isSynced) VALUES (1, 'Squat', 'kg', 0, 0)")
            db.execSQL("INSERT INTO metric_entries (id, metricId, value, loggedAt, isSynced) VALUES (1, 1, 100.0, 0, 0)")
        }

        helper.runMigrationsAndValidate(dbName, 2, true).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            val repository = GymRepository(db.metricDao(), db.entryDao(), db.workoutDao())
            runBlocking {
                val metrics = repository.getAllMetricsWithMax().first()
                assertEquals(listOf("Squat"), metrics.map { it.metric.name })
                assertEquals(100.0, metrics.single().maxValue!!, 0.0)

                repository.saveWorkout(Workout(name = "Push", type = WorkoutType.MAIN, rotationOrder = 1), emptyList())
                assertEquals("Push", repository.getSchedule().first().first().workout.workout.name)
            }
        } finally {
            db.close()
        }
    }
}
