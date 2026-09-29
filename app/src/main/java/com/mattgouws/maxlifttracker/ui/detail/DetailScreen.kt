package com.mattgouws.maxlifttracker.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.MetricEntry
import com.mattgouws.maxlifttracker.data.TrackedMetric
import com.mattgouws.maxlifttracker.ui.formatDateTime
import com.mattgouws.maxlifttracker.ui.formatValue
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The log-entry flow is wired up in #8.
    DetailContent(uiState = uiState, onBack = onBack, onLogEntry = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onLogEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.metric?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onLogEntry) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.log_entry))
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        val metric = uiState.metric
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            metric == null -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.metric_not_found), textAlign = TextAlign.Center)
            }

            uiState.entries.isEmpty() -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.detail_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(contentModifier) {
                items(uiState.entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = {
                            Text(formatValue(entry.value, metric.unit), style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = { Text(formatDateTime(entry.loggedAt)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailEmptyPreview() {
    MaxLiftTrackerTheme {
        DetailContent(
            DetailUiState(metric = TrackedMetric(id = 1, name = "Squat"), isLoading = false),
            onBack = {},
            onLogEntry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailWithEntriesPreview() {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L
    MaxLiftTrackerTheme {
        DetailContent(
            DetailUiState(
                metric = TrackedMetric(id = 1, name = "Squat"),
                entries = listOf(
                    MetricEntry(id = 3, metricId = 1, value = 82.5, loggedAt = now),
                    MetricEntry(id = 2, metricId = 1, value = 80.0, loggedAt = now - 3 * day),
                    MetricEntry(id = 1, metricId = 1, value = 75.0, loggedAt = now - 7 * day),
                ),
                isLoading = false,
            ),
            onBack = {},
            onLogEntry = {},
        )
    }
}
