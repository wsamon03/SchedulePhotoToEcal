package com.wsamon.schedulephototoecal.ui.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.parser.TimeRangeParser
import java.time.format.DateTimeFormatter

private val EDIT_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun ShiftEditDialog(
    shift: ParsedShift,
    onDismiss: () -> Unit,
    onSave: (ParsedShift) -> Unit,
) {
    var timeRangeText by remember {
        val startTime = shift.startTime
        val endTime = shift.endTime
        mutableStateOf(
            if (startTime != null && endTime != null) {
                "${startTime.format(EDIT_TIME_FORMATTER)} - ${endTime.format(EDIT_TIME_FORMATTER)}"
            } else {
                ""
            },
        )
    }
    var position by remember { mutableStateOf(shift.position.orEmpty()) }
    var storeNumber by remember { mutableStateOf(shift.storeNumber.orEmpty()) }
    var timeError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit shift") },
        text = {
            Column {
                OutlinedTextField(
                    value = timeRangeText,
                    onValueChange = {
                        timeRangeText = it
                        timeError = false
                    },
                    label = { Text("Time range (e.g. 9:00 AM - 5:00 PM)") },
                    isError = timeError,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Position") },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = storeNumber,
                    onValueChange = { storeNumber = it },
                    label = { Text("Store number") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedRange = TimeRangeParser.parse(timeRangeText)
                if (parsedRange == null) {
                    timeError = true
                } else {
                    onSave(
                        shift.copy(
                            startTime = parsedRange.start,
                            endTime = parsedRange.end,
                            position = position.ifBlank { null },
                            storeNumber = storeNumber.ifBlank { null },
                            notScheduled = false,
                            notFoundInPhoto = false,
                            included = true,
                        ),
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
