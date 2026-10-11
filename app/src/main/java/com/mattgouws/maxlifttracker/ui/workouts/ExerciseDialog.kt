package com.mattgouws.maxlifttracker.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.ui.home.MAX_NAME_LENGTH
import com.mattgouws.maxlifttracker.ui.parseCount
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

/** Adds an exercise, or edits [initial] when given. */
@Composable
fun ExerciseDialog(
    initial: ExerciseDraft?,
    onConfirm: (name: String, sets: Int, reps: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var setsText by rememberSaveable { mutableStateOf(initial?.sets?.toString() ?: "3") }
    var repsText by rememberSaveable { mutableStateOf(initial?.reps?.toString() ?: "10") }
    val sets = parseCount(setsText)
    val reps = parseCount(repsText)
    val canSave = name.isNotBlank() && sets != null && reps != null
    val confirm = { if (sets != null && reps != null && name.isNotBlank()) onConfirm(name, sets, reps) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_exercise else R.string.edit_exercise)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.metric_name)) },
                    placeholder = { Text(stringResource(R.string.exercise_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CountField(setsText, { setsText = it }, R.string.sets, ImeAction.Next, {}, Modifier.weight(1f))
                    CountField(repsText, { repsText = it }, R.string.reps, ImeAction.Done, confirm, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = canSave) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CountField(
    text: String,
    onTextChange: (String) -> Unit,
    label: Int,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showError = parseCount(text) == null
    OutlinedTextField(
        value = text,
        onValueChange = { onTextChange(it.filter(Char::isDigit).take(3)) },
        label = { Text(stringResource(label)) },
        isError = showError,
        supportingText = if (showError) {
            { Text(stringResource(R.string.count_error)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ExerciseDialogPreview() {
    MaxLiftTrackerTheme {
        ExerciseDialog(initial = null, onConfirm = { _, _, _ -> }, onDismiss = {})
    }
}
