package com.mattgouws.maxlifttracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.MetricEntry
import com.mattgouws.maxlifttracker.data.TrackedMetric
import com.mattgouws.maxlifttracker.ui.MetricDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val metric: TrackedMetric? = null,
    val entries: List<MetricEntry> = emptyList(),
    // True until the first database read arrives, so the empty state doesn't flash on open.
    val isLoading: Boolean = true,
) {
    /** True if [value] beats every existing entry. The first entry doesn't count as a new max. */
    fun isNewMax(value: Double): Boolean = entries.isNotEmpty() && value > entries.maxOf { it.value }
}

class DetailViewModel(
    private val metricId: Long,
    private val repository: GymRepository,
) : ViewModel() {
    val uiState: StateFlow<DetailUiState> = combine(
        repository.getMetric(metricId),
        repository.getEntriesForMetric(metricId),
    ) { metric, entries ->
        DetailUiState(metric = metric, entries = entries, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun logEntry(value: Double) {
        viewModelScope.launch { repository.logEntry(metricId, value) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }

    companion object {
        fun factory(repository: GymRepository) = viewModelFactory {
            initializer {
                val route = createSavedStateHandle().toRoute<MetricDetailRoute>()
                DetailViewModel(route.metricId, repository)
            }
        }
    }
}
