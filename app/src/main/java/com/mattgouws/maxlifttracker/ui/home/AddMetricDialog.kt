package com.mattgouws.maxlifttracker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mattgouws.maxlifttracker.R
import com.mattgouws.maxlifttracker.ui.theme.MaxLiftTrackerTheme

val WeightUnits = listOf("kg", "lb")

const val MAX_NAME_LENGTH = 40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMetricDialog(
    existingNames: List<String>,
    onConfirm: (name: String, unit: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(WeightUnits.first()) }
    val isDuplicate = existingNames.any { it.equals(name.trim(), ignoreCase = true) }
    val canSave = name.isNotBlank() && !isDuplicate
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_metric)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.metric_name)) },
                    placeholder = { Text(stringResource(R.string.metric_name_hint)) },
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text(stringResource(R.string.metric_name_duplicate, name.trim())) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) onConfirm(name, unit) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Text(
                    text = stringResource(R.string.unit),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WeightUnits.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = unit == option,
                            onClick = { unit = option },
                            shape = SegmentedButtonDefaults.itemShape(index, WeightUnits.size),
                        ) {
                            Text(option)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, unit) }, enabled = canSave) {
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

@Preview
@Composable
private fun AddMetricDialogPreview() {
    MaxLiftTrackerTheme {
        AddMetricDialog(existingNames = emptyList(), onConfirm = { _, _ -> }, onDismiss = {})
    }
}
