package com.mattgouws.maxlifttracker.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.ScheduledWorkout
import com.mattgouws.maxlifttracker.data.WorkoutCompletion
import com.mattgouws.maxlifttracker.data.WorkoutType
import com.mattgouws.maxlifttracker.data.WorkoutWithExercises
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val workouts: List<WorkoutWithExercises> = emptyList(),
    // Up next first. Empty when there's nothing to do.
    val schedule: List<ScheduledWorkout> = emptyList(),
    val latestCompletion: WorkoutCompletion? = null,
    // True until the first database read arrives, so the empty state doesn't flash on launch.
    val isLoading: Boolean = true,
) {
    val upNext: ScheduledWorkout? get() = schedule.firstOrNull()
    val mainWorkouts: List<WorkoutWithExercises> get() = workouts.filter { it.workout.type == WorkoutType.MAIN }
    val adhocWorkouts: List<WorkoutWithExercises> get() = workouts.filter { it.workout.type == WorkoutType.ADHOC }
}

class WorkoutsViewModel(private val repository: GymRepository) : ViewModel() {
    val uiState: StateFlow<WorkoutsUiState> = combine(
        repository.getAllWorkouts(),
        repository.getSchedule(),
        repository.getLatestCompletion(),
    ) { workouts, schedule, latest ->
        WorkoutsUiState(workouts, schedule, latest, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    fun complete(scheduled: ScheduledWorkout) {
        viewModelScope.launch { repository.completeWorkout(scheduled) }
    }

    fun skip(scheduled: ScheduledWorkout) {
        viewModelScope.launch { repository.skipWorkout(scheduled) }
    }

    /** Queues an adhoc workout to be done once [mainWorkoutsBefore] more main workouts are done. */
    fun queue(workoutId: Long, mainWorkoutsBefore: Int) {
        viewModelScope.launch { repository.queueWorkout(workoutId, mainWorkoutsBefore) }
    }

    fun removeFromQueue(queueEntryId: Long) {
        viewModelScope.launch { repository.removeFromQueue(queueEntryId) }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch { repository.deleteWorkout(id) }
    }

    companion object {
        fun factory(repository: GymRepository) = viewModelFactory {
            initializer { WorkoutsViewModel(repository) }
        }
    }
}
