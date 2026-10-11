package com.mattgouws.maxlifttracker.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.Workout
import com.mattgouws.maxlifttracker.data.WorkoutExercise
import com.mattgouws.maxlifttracker.data.WorkoutType
import com.mattgouws.maxlifttracker.ui.NEW_WORKOUT_ID
import com.mattgouws.maxlifttracker.ui.WorkoutEditorRoute
import com.mattgouws.maxlifttracker.ui.parseCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An exercise being edited. [key] identifies it in the list, since names can repeat. */
data class ExerciseDraft(val key: Int, val name: String, val sets: Int, val reps: Int)

data class WorkoutEditorUiState(
    val isNew: Boolean,
    // True until an existing workout has loaded.
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val name: String = "",
    val type: WorkoutType = WorkoutType.MAIN,
    val orderText: String = "",
    val exercises: List<ExerciseDraft> = emptyList(),
    // Every workout except this one, for duplicate checks.
    val otherWorkouts: List<Workout> = emptyList(),
    val isSaving: Boolean = false,
) {
    val isDuplicateName: Boolean get() = otherWorkouts.any { it.name.equals(name.trim(), ignoreCase = true) }

    val order: Int? get() = parseCount(orderText)

    /** The main workout already using [order], if any. */
    val orderTakenBy: Workout?
        get() = order?.let { order ->
            otherWorkouts.find { it.type == WorkoutType.MAIN && it.rotationOrder == order }
        }

    val canSave: Boolean
        get() = !isLoading && !notFound && !isSaving && name.isNotBlank() && !isDuplicateName &&
            (type == WorkoutType.ADHOC || (order != null && orderTakenBy == null))
}

class WorkoutEditorViewModel(
    private val workoutId: Long,
    private val repository: GymRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutEditorUiState(isNew = workoutId == NEW_WORKOUT_ID))
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    // The workout as loaded, so saving keeps the fields this screen doesn't edit.
    private var original: Workout? = null
    private var nextKey = 0

    init {
        viewModelScope.launch {
            val all = repository.getAllWorkouts().first()
            val existing = all.find { it.workout.id == workoutId }
            val others = all.map { it.workout }.filter { it.id != workoutId }
            original = existing?.workout
            _uiState.update { state ->
                when {
                    existing != null -> state.copy(
                        isLoading = false,
                        name = existing.workout.name,
                        type = existing.workout.type,
                        orderText = existing.workout.rotationOrder?.toString() ?: defaultOrder(others),
                        exercises = existing.exercises.map { ExerciseDraft(nextKey++, it.name, it.sets, it.reps) },
                        otherWorkouts = others,
                    )

                    state.isNew -> state.copy(isLoading = false, orderText = defaultOrder(others), otherWorkouts = others)
                    else -> state.copy(isLoading = false, notFound = true)
                }
            }
        }
    }

    /** One after the last main workout, so a new one joins the end of the rotation. */
    private fun defaultOrder(others: List<Workout>): String =
        ((others.filter { it.type == WorkoutType.MAIN }.mapNotNull { it.rotationOrder }.maxOrNull() ?: 0) + 1)
            .toString()

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setType(type: WorkoutType) = _uiState.update { it.copy(type = type) }

    fun setOrderText(text: String) = _uiState.update { it.copy(orderText = text.filter(Char::isDigit).take(3)) }

    fun addExercise(name: String, sets: Int, reps: Int) = _uiState.update {
        it.copy(exercises = it.exercises + ExerciseDraft(nextKey++, name.trim(), sets, reps))
    }

    fun updateExercise(key: Int, name: String, sets: Int, reps: Int) = _uiState.update { state ->
        state.copy(
            exercises = state.exercises.map {
                if (it.key == key) it.copy(name = name.trim(), sets = sets, reps = reps) else it
            },
        )
    }

    fun removeExercise(key: Int) = _uiState.update { state ->
        state.copy(exercises = state.exercises.filterNot { it.key == key })
    }

    /** Moves an exercise [offset] places up (negative) or down (positive), if there's room. */
    fun moveExercise(key: Int, offset: Int) = _uiState.update { state ->
        val from = state.exercises.indexOfFirst { it.key == key }
        val to = from + offset
        if (from < 0 || to !in state.exercises.indices) return@update state
        val exercises = state.exercises.toMutableList()
        exercises.add(to, exercises.removeAt(from))
        state.copy(exercises = exercises)
    }

    /** Saves the workout, then calls [onSaved] with its name. Does nothing if it isn't valid. */
    fun save(onSaved: (name: String) -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        val name = state.name.trim()
        val rotationOrder = if (state.type == WorkoutType.MAIN) state.order else null
        val workout = original?.copy(name = name, type = state.type, rotationOrder = rotationOrder)
            ?: Workout(name = name, type = state.type, rotationOrder = rotationOrder)
        val exercises = state.exercises.map { WorkoutExercise(name = it.name, sets = it.sets, reps = it.reps) }
        viewModelScope.launch {
            repository.saveWorkout(workout, exercises)
            onSaved(name)
        }
    }

    companion object {
        fun factory(repository: GymRepository) = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<WorkoutEditorRoute>()
                WorkoutEditorViewModel(route.workoutId, repository)
            }
        }
    }
}
