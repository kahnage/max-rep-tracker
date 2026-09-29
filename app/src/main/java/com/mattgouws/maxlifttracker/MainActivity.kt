package com.mattgouws.maxlifttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mattgouws.maxlifttracker.ui.home.HomeScreen
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaxLiftTrackerTheme {
                // The add-metric flow is wired up in #6.
                HomeScreen(onAddMetric = {})
            }
        }
    }
}
