package com.mattgouws.maxlifttracker.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.ui.detail.DetailScreen
import com.mattgouws.maxlifttracker.ui.detail.DetailViewModel
import com.mattgouws.maxlifttracker.ui.home.HomeScreen
import com.mattgouws.maxlifttracker.ui.home.HomeViewModel
import com.mattgouws.maxlifttracker.ui.workouts.WorkoutEditorScreen
import com.mattgouws.maxlifttracker.ui.workouts.WorkoutEditorViewModel
import com.mattgouws.maxlifttracker.ui.workouts.WorkoutsScreen
import com.mattgouws.maxlifttracker.ui.workouts.WorkoutsViewModel
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** The max lifts tab. */
@Serializable
object HomeRoute

@Serializable
data class MetricDetailRoute(val metricId: Long)

@Serializable
object WorkoutsRoute

const val NEW_WORKOUT_ID = 0L

@Serializable
data class WorkoutEditorRoute(val workoutId: Long = NEW_WORKOUT_ID)

// Passes the name of a just-saved workout back from the editor to Workouts.
private const val SAVED_WORKOUT_KEY = "saved_workout"

private enum class Tab(val route: Any, val routeClass: KClass<*>, @StringRes val label: Int, @DrawableRes val icon: Int) {
    WORKOUTS(WorkoutsRoute, WorkoutsRoute::class, R.string.tab_workouts, R.drawable.ic_fitness_center),
    MAX_LIFTS(HomeRoute, HomeRoute::class, R.string.tab_max_lifts, R.drawable.ic_trending_up),
}

@Composable
fun AppNavHost(repository: GymRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    // Tabs show only on the top-level screens, not ones pushed on top of them.
    val showTabs = destination == null || Tab.entries.any { destination.hasRoute(it.routeClass) }

    Scaffold(
        // Each screen handles its own insets; this only makes room for the tab bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WorkoutsRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<WorkoutsRoute> { entry ->
                val savedWorkoutName by entry.savedStateHandle
                    .getStateFlow<String?>(SAVED_WORKOUT_KEY, null)
                    .collectAsStateWithLifecycle()
                WorkoutsScreen(
                    viewModel = viewModel(factory = WorkoutsViewModel.factory(repository)),
                    onAddWorkout = { navController.navigate(WorkoutEditorRoute()) },
                    onWorkoutClick = { id -> navController.navigate(WorkoutEditorRoute(id)) },
                    savedWorkoutName = savedWorkoutName,
                    onSavedMessageShown = { entry.savedStateHandle[SAVED_WORKOUT_KEY] = null },
                )
            }
            composable<WorkoutEditorRoute> {
                WorkoutEditorScreen(
                    viewModel = viewModel(factory = WorkoutEditorViewModel.factory(repository)),
                    onBack = { navController.popBackStack() },
                    onSaved = { name ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(SAVED_WORKOUT_KEY, name)
                        // Not a plain popBackStack(): if the user already went back, that would pop Workouts.
                        navController.popBackStack<WorkoutEditorRoute>(inclusive = true)
                    },
                )
            }
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
}
