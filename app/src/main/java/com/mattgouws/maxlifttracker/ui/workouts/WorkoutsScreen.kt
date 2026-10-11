package com.mattgouws.maxlifttracker.ui.workouts

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.QueuedWorkout
import com.mattgouws.maxlifttracker.data.ScheduledWorkout
import com.mattgouws.maxlifttracker.data.Workout
import com.mattgouws.maxlifttracker.data.WorkoutCompletion
import com.mattgouws.maxlifttracker.data.WorkoutExercise
import com.mattgouws.maxlifttracker.data.WorkoutType
import com.mattgouws.maxlifttracker.data.WorkoutWithExercises
import com.mattgouws.maxlifttracker.data.buildSchedule
import com.mattgouws.maxlifttracker.ui.ConfirmDeleteDialog
import com.mattgouws.maxlifttracker.ui.formatDateTime
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme
import kotlinx.coroutines.launch

@Composable
fun WorkoutsScreen(
    viewModel: WorkoutsViewModel,
    onAddWorkout: () -> Unit,
    onWorkoutClick: (workoutId: Long) -> Unit,
    // Name of a workout just saved in the editor, to confirm with a snackbar.
    savedWorkoutName: String? = null,
    onSavedMessageShown: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCompleteDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Replaces any message still showing, so quick actions in a row don't queue up stale ones.
    fun showMessage(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(savedWorkoutName) {
        if (savedWorkoutName != null) {
            showMessage(context.getString(R.string.workout_saved, savedWorkoutName))
            onSavedMessageShown()
        }
    }

    WorkoutsContent(
        uiState = uiState,
        onAddWorkout = onAddWorkout,
        onWorkoutClick = onWorkoutClick,
        onWorkoutLongClick = { id -> pendingDeleteId = id },
        onComplete = { showCompleteDialog = true },
        onQueue = { workout, mainWorkoutsBefore ->
            viewModel.queue(workout.id, mainWorkoutsBefore)
            val message = if (mainWorkoutsBefore == 0) R.string.queued_next else R.string.queued_in_two
            showMessage(context.getString(message, workout.name))
        },
        onRemoveFromQueue = { scheduled ->
            scheduled.queueEntryId?.let(viewModel::removeFromQueue)
            showMessage(context.getString(R.string.removed_from_queue, scheduled.workout.workout.name))
        },
        snackbarHostState = snackbarHostState,
    )

    val upNext = uiState.upNext
    if (showCompleteDialog && upNext != null) {
        val name = upNext.workout.workout.name
        CompleteWorkoutDialog(
            workoutName = name,
            onConfirm = {
                viewModel.complete(upNext)
                showCompleteDialog = false
                showMessage(context.getString(R.string.workout_completed, name))
            },
            onDismiss = { showCompleteDialog = false },
        )
    }

    val pendingDelete = uiState.workouts.find { it.workout.id == pendingDeleteId }?.workout
    if (pendingDelete != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_metric_title, pendingDelete.name),
            text = stringResource(R.string.delete_workout_text),
            onConfirm = {
                viewModel.deleteWorkout(pendingDelete.id)
                pendingDeleteId = null
                showMessage(context.getString(R.string.workout_deleted, pendingDelete.name))
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsContent(
    uiState: WorkoutsUiState,
    onAddWorkout: () -> Unit,
    onWorkoutClick: (workoutId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onWorkoutLongClick: (workoutId: Long) -> Unit = {},
    onComplete: () -> Unit = {},
    onQueue: (workout: Workout, mainWorkoutsBefore: Int) -> Unit = { _, _ -> },
    onRemoveFromQueue: (ScheduledWorkout) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_workouts)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWorkout) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_workout),
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

            uiState.workouts.isEmpty() -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.workouts_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(contentModifier) {
                item(key = "up_next") {
                    val upNext = uiState.upNext
                    if (upNext != null) {
                        UpNextCard(
                            scheduled = upNext,
                            latestCompletion = uiState.latestCompletion,
                            onComplete = onComplete,
                            onRemoveFromQueue = { onRemoveFromQueue(upNext) },
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_main_workouts),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        )
                    }
                }

                val comingUp = uiState.schedule.drop(1)
                if (comingUp.isNotEmpty()) {
                    sectionHeader(R.string.coming_up)
                    // A workout can appear more than once, so key by position.
                    itemsIndexed(comingUp, key = { index, _ -> "coming_up_$index" }) { _, scheduled ->
                        ListItem(
                            headlineContent = { WorkoutName(scheduled.workout.workout) },
                            supportingContent = { Text(workoutLabel(scheduled)) },
                            trailingContent = if (scheduled.queueEntryId != null) {
                                {
                                    IconButton(onClick = { onRemoveFromQueue(scheduled) }) {
                                        Icon(
                                            painterResource(R.drawable.ic_close),
                                            contentDescription = stringResource(R.string.remove_from_queue),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }

                if (uiState.mainWorkouts.isNotEmpty()) {
                    sectionHeader(R.string.main_rotation)
                    items(uiState.mainWorkouts, key = { it.workout.id }) { item ->
                        WorkoutRow(item, onWorkoutClick, onWorkoutLongClick)
                    }
                }

                if (uiState.adhocWorkouts.isNotEmpty()) {
                    sectionHeader(R.string.adhoc_workouts)
                    items(uiState.adhocWorkouts, key = { it.workout.id }) { item ->
                        WorkoutRow(item, onWorkoutClick, onWorkoutLongClick) {
                            QueueMenu(onQueue = { mainWorkoutsBefore -> onQueue(item.workout, mainWorkoutsBefore) })
                        }
                    }
                }

                // Keeps the last row clear of the FAB.
                item(key = "fab_spacer") { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

private fun LazyListScope.sectionHeader(title: Int) {
    item(key = "header_$title") {
        HorizontalDivider()
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun WorkoutName(workout: Workout) {
    Text(workout.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun workoutLabel(scheduled: ScheduledWorkout): String {
    val workout = scheduled.workout.workout
    return when {
        workout.type == WorkoutType.MAIN -> stringResource(R.string.main_workout_label, workout.rotationOrder ?: 0)
        scheduled.queueEntryId != null -> stringResource(R.string.queued_adhoc_label)
        else -> stringResource(R.string.adhoc_workout_label)
    }
}

@Composable
private fun exerciseSummary(exercises: List<WorkoutExercise>): String =
    if (exercises.isEmpty()) {
        stringResource(R.string.no_exercises)
    } else {
        pluralStringResource(R.plurals.exercise_count, exercises.size, exercises.size)
    }

@Composable
private fun UpNextCard(
    scheduled: ScheduledWorkout,
    latestCompletion: WorkoutCompletion?,
    onComplete: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workout = scheduled.workout.workout
    val exercises = scheduled.workout.exercises
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.up_next),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(workout.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = workoutLabel(scheduled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (exercises.isEmpty()) {
                Text(stringResource(R.string.no_exercises), style = MaterialTheme.typography.bodyMedium)
            }
            exercises.forEach { exercise ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.sets_by_reps, exercise.sets, exercise.reps),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (latestCompletion != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.last_completed,
                        latestCompletion.workoutName,
                        formatDateTime(latestCompletion.completedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (scheduled.queueEntryId != null) {
                    TextButton(onClick = onRemoveFromQueue) { Text(stringResource(R.string.remove_from_queue)) }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = onComplete, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.mark_complete))
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    item: WorkoutWithExercises,
    onClick: (workoutId: Long) -> Unit,
    onLongClick: (workoutId: Long) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val workout = item.workout
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { onClick(workout.id) },
            onLongClick = { onLongClick(workout.id) },
            onLongClickLabel = stringResource(R.string.delete),
        ),
        overlineContent = if (workout.type == WorkoutType.MAIN) {
            { Text("#${workout.rotationOrder}") }
        } else {
            null
        },
        headlineContent = { WorkoutName(workout) },
        supportingContent = { Text(exerciseSummary(item.exercises)) },
        trailingContent = trailing,
    )
}

@Composable
private fun QueueMenu(onQueue: (mainWorkoutsBefore: Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_playlist_add), contentDescription = stringResource(R.string.queue))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.queue_next)) },
                onClick = {
                    expanded = false
                    onQueue(0)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.queue_in_two)) },
                onClick = {
                    expanded = false
                    onQueue(1)
                },
            )
        }
    }
}

@Composable
private fun CompleteWorkoutDialog(workoutName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.complete_workout_title, workoutName)) },
        text = { Text(stringResource(R.string.complete_workout_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.complete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private val previewWorkouts = listOf(
    WorkoutWithExercises(
        Workout(id = 1, name = "Push", type = WorkoutType.MAIN, rotationOrder = 1),
        listOf(
            WorkoutExercise(name = "Bench press", sets = 4, reps = 8),
            WorkoutExercise(name = "Overhead press", sets = 3, reps = 10),
        ),
    ),
    WorkoutWithExercises(Workout(id = 2, name = "Pull", type = WorkoutType.MAIN, rotationOrder = 2), emptyList()),
    WorkoutWithExercises(Workout(id = 3, name = "Legs", type = WorkoutType.MAIN, rotationOrder = 3), emptyList()),
    WorkoutWithExercises(
        Workout(id = 4, name = "Cardio", type = WorkoutType.ADHOC),
        listOf(WorkoutExercise(name = "Rower", sets = 1, reps = 20)),
    ),
)

@Preview(showBackground = true)
@Composable
private fun WorkoutsEmptyPreview() {
    MaxLiftTrackerTheme {
        WorkoutsContent(WorkoutsUiState(isLoading = false), onAddWorkout = {}, onWorkoutClick = {})
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun WorkoutsWithQueuePreview() {
    MaxLiftTrackerTheme {
        WorkoutsContent(
            WorkoutsUiState(
                workouts = previewWorkouts,
                schedule = buildSchedule(previewWorkouts, listOf(QueuedWorkout(1, 4, 1)), lastCompletedMainOrder = 3),
                latestCompletion = WorkoutCompletion(
                    workoutId = 3,
                    workoutName = "Legs",
                    workoutType = WorkoutType.MAIN,
                    rotationOrder = 3,
                ),
                isLoading = false,
            ),
            onAddWorkout = {},
            onWorkoutClick = {},
        )
    }
}
