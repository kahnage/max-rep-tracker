package com.mattgouws.maxlifttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mattgouws.maxlifttracker.ui.AppNavHost
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MaxLiftTrackerApplication).repository
        setContent {
            MaxLiftTrackerTheme {
                AppNavHost(repository)
            }
        }
    }
}
