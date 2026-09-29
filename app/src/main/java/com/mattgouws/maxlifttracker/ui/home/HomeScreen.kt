package com.mattgouws.maxlifttracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.MetricWithMax
import com.mattgouws.maxlifttracker.data.TrackedMetric
import com.mattgouws.maxlifttracker.ui.formatValue
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMetricClick: (metricId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    HomeContent(uiState = uiState, onAddMetric = { showAddDialog = true }, onMetricClick = onMetricClick)

    if (showAddDialog) {
        AddMetricDialog(
            onConfirm = { name, unit ->
                viewModel.addMetric(name, unit)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onAddMetric: () -> Unit,
    onMetricClick: (metricId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMetric) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_metric),
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.metrics.isEmpty() -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.home_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(contentModifier) {
                items(uiState.metrics, key = { it.metric.id }) { item ->
                    MetricRow(item, onClick = { onMetricClick(item.metric.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MetricRow(item: MetricWithMax, onClick: () -> Unit) {
    val max = item.maxValue
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.metric.name) },
        trailingContent = {
            Text(
                text = if (max != null) formatValue(max, item.metric.unit) else stringResource(R.string.no_entries),
                style = MaterialTheme.typography.titleMedium,
            )
        },
    )
}

private val previewMetrics = listOf(
    MetricWithMax(TrackedMetric(id = 1, name = "Bench Press"), 62.5),
    MetricWithMax(TrackedMetric(id = 2, name = "Deadlift", unit = "lb"), 225.0),
    MetricWithMax(TrackedMetric(id = 3, name = "Squat"), null),
)

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    MaxLiftTrackerTheme {
        HomeContent(HomeUiState(isLoading = false), onAddMetric = {}, onMetricClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSinglePreview() {
    MaxLiftTrackerTheme {
        HomeContent(HomeUiState(previewMetrics.take(1), isLoading = false), onAddMetric = {}, onMetricClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeMultiplePreview() {
    MaxLiftTrackerTheme {
        HomeContent(HomeUiState(previewMetrics, isLoading = false), onAddMetric = {}, onMetricClick = {})
    }
}
