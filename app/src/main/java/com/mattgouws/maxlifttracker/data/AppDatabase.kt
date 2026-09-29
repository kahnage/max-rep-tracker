package com.mattgouws.maxlifttracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedMetric::class, MetricEntry::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase()
