package com.mattgouws.maxlifttracker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.ui.detail.DetailScreen
import com.mattgouws.maxlifttracker.ui.detail.DetailViewModel
import com.mattgouws.maxlifttracker.ui.home.HomeScreen
import com.mattgouws.maxlifttracker.ui.home.HomeViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class MetricDetailRoute(val metricId: Long)

@Composable
fun AppNavHost(repository: GymRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                viewModel = viewModel(factory = HomeViewModel.factory(repository)),
                onMetricClick = { id -> navController.navigate(MetricDetailRoute(id)) },
            )
        }
        composable<MetricDetailRoute> {
            DetailScreen(
                viewModel = viewModel(factory = DetailViewModel.factory(repository)),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
