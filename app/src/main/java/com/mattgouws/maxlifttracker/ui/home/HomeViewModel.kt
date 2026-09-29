package com.mattgouws.maxlifttracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mattgouws.maxlifttracker.data.GymRepository
import com.mattgouws.maxlifttracker.data.MetricWithMax
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val metrics: List<MetricWithMax> = emptyList(),
    // True until the first database read arrives, so the empty state doesn't flash on launch.
    val isLoading: Boolean = true,
)

class HomeViewModel(private val repository: GymRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = repository.getAllMetricsWithMax()
        .map { HomeUiState(metrics = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun addMetric(name: String, unit: String) {
        viewModelScope.launch { repository.addMetric(name.trim(), unit) }
    }

    companion object {
        fun factory(repository: GymRepository) = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
