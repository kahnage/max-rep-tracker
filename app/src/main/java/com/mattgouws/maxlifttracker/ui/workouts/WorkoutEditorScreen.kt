package com.mattgouws.maxlifttracker.ui.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.data.WorkoutType
import com.mattgouws.maxlifttracker.ui.home.MAX_NAME_LENGTH
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

// Key for the exercise dialog when it's adding rather than editing.
private const val NEW_EXERCISE = -1

@Composable
fun WorkoutEditorScreen(
    viewModel: WorkoutEditorViewModel,
    onBack: () -> Unit,
    onSaved: (name: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // NEW_EXERCISE to add one, an exercise's key to edit it, or null when closed.
    var dialogKey by rememberSaveable { mutableStateOf<Int?>(null) }

    WorkoutEditorContent(
        uiState = uiState,
        onBack = onBack,
        onSave = { viewModel.save(onSaved) },
        onNameChange = viewModel::setName,
        onTypeChange = viewModel::setType,
        onOrderChange = viewModel::setOrderText,
        onAddExercise = { dialogKey = NEW_EXERCISE },
        onExerciseClick = { key -> dialogKey = key },
        onMoveExercise = viewModel::moveExercise,
        onRemoveExercise = viewModel::removeExercise,
    )

    val key = dialogKey ?: return
    val editing = uiState.exercises.find { it.key == key }
    if (key == NEW_EXERCISE || editing != null) {
        ExerciseDialog(
            initial = editing,
            onConfirm = { name, sets, reps ->
                if (editing == null) viewModel.addExercise(name, sets, reps) else viewModel.updateExercise(key, name, sets, reps)
                dialogKey = null
            },
            onDismiss = { dialogKey = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorContent(
    uiState: WorkoutEditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit = {},
    onTypeChange: (WorkoutType) -> Unit = {},
    onOrderChange: (String) -> Unit = {},
    onAddExercise: () -> Unit = {},
    onExerciseClick: (key: Int) -> Unit = {},
    onMoveExercise: (key: Int, offset: Int) -> Unit = { _, _ -> },
    onRemoveExercise: (key: Int) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.new_workout else R.string.edit_workout)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = uiState.canSave) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            uiState.notFound -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.workout_not_found), textAlign = TextAlign.Center)
            }

            else -> LazyColumn(
                contentModifier
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
            ) {
                item(key = "details") {
                    WorkoutDetailsFields(uiState, onNameChange, onTypeChange, onOrderChange)
                }
                item(key = "exercises_header") {
                    Text(
                        text = stringResource(R.string.exercises),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(uiState.exercises, key = { _, it -> it.key }) { index, exercise ->
                    ListItem(
                        modifier = Modifier.clickable { onExerciseClick(exercise.key) },
                        headlineContent = { Text(exercise.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(stringResource(R.string.sets_by_reps, exercise.sets, exercise.reps))
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onMoveExercise(exercise.key, -1) }, enabled = index > 0) {
                                    Icon(painterResource(R.drawable.ic_arrow_upward), stringResource(R.string.move_up))
                                }
                                IconButton(
                                    onClick = { onMoveExercise(exercise.key, 1) },
                                    enabled = index < uiState.exercises.lastIndex,
                                ) {
                                    Icon(painterResource(R.drawable.ic_arrow_downward), stringResource(R.string.move_down))
                                }
                                IconButton(onClick = { onRemoveExercise(exercise.key) }) {
                                    Icon(
                                        painterResource(R.drawable.ic_close),
                                        stringResource(R.string.remove_exercise, exercise.name),
                                    )
                                }
                            }
                        },
                    )
                }
                item(key = "add_exercise") {
                    OutlinedButton(
                        onClick = onAddExercise,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.add_exercise))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetailsFields(
    uiState: WorkoutEditorUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (WorkoutType) -> Unit,
    onOrderChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    if (uiState.isNew) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onNameChange(it.take(MAX_NAME_LENGTH)) },
            label = { Text(stringResource(R.string.metric_name)) },
            placeholder = { Text(stringResource(R.string.workout_name_hint)) },
            isError = uiState.isDuplicateName,
            supportingText = if (uiState.isDuplicateName) {
                { Text(stringResource(R.string.workout_name_duplicate, uiState.name.trim())) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        Text(
            text = stringResource(R.string.workout_type),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        val types = listOf(WorkoutType.MAIN to R.string.type_main, WorkoutType.ADHOC to R.string.type_adhoc)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = uiState.type == type,
                    onClick = { onTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                ) {
                    Text(stringResource(label))
                }
            }
        }
        if (uiState.type == WorkoutType.MAIN) {
            val takenBy = uiState.orderTakenBy
            val isError = uiState.order == null || takenBy != null
            OutlinedTextField(
                value = uiState.orderText,
                onValueChange = onOrderChange,
                label = { Text(stringResource(R.string.rotation_order)) },
                isError = isError,
                supportingText = {
                    Text(
                        when {
                            uiState.order == null -> stringResource(R.string.rotation_order_error)
                            takenBy != null -> stringResource(R.string.rotation_order_taken, takenBy.name)
                            else -> stringResource(R.string.rotation_order_help)
                        },
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutEditorPreview() {
    MaxLiftTrackerTheme {
        WorkoutEditorContent(
            WorkoutEditorUiState(
                isNew = false,
                isLoading = false,
                name = "Push",
                orderText = "1",
                exercises = listOf(
                    ExerciseDraft(0, "Bench press", 4, 8),
                    ExerciseDraft(1, "Overhead press", 3, 10),
                ),
            ),
            onBack = {},
            onSave = {},
        )
    }
}
