package com.mattgouws.maxlifttracker

import android.app.Application
import com.mattgouws.maxlifttracker.data.AppDatabase
import com.mattgouws.maxlifttracker.data.GymRepository

class MaxLiftTrackerApplication : Application() {
    // Created on first use so the database isn't opened until something needs it.
    val repository: GymRepository by lazy {
        val db = AppDatabase.getInstance(this)
        GymRepository(db.metricDao(), db.entryDao())
    }
}
