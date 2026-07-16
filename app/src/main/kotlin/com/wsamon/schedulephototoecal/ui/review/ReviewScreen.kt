package com.wsamon.schedulephototoecal.ui.review

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.util.PermissionUtils
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CALENDAR_PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun ReviewScreen(
    viewModel: ScheduleImportViewModel,
    onImported: () -> Unit,
) {
    val context = LocalContext.current
    var calendarPermissionGranted by remember {
        mutableStateOf(PermissionUtils.allGranted(context, CALENDAR_PERMISSIONS))
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ParsedShift?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        calendarPermissionGranted = results.values.all { it }
        if (calendarPermissionGranted) {
            viewModel.refreshAvailableCalendars()
        } else {
            permissionDeniedOnce = true
        }
    }

    LaunchedEffect(Unit) {
        if (calendarPermissionGranted) {
            viewModel.refreshAvailableCalendars()
        } else {
            permissionLauncher.launch(CALENDAR_PERMISSIONS)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Review your shifts", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.parseResult?.warnings?.forEach { warning ->
            Text(warning, color = MaterialTheme.colorScheme.error)
        }

        if (!calendarPermissionGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Calendar permission is needed to add these shifts to your calendar.")
            Spacer(modifier = Modifier.height(4.dp))
            if (permissionDeniedOnce) {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open App Settings")
                }
            } else {
                Button(onClick = { permissionLauncher.launch(CALENDAR_PERMISSIONS) }) {
                    Text("Grant Calendar Permission")
                }
            }
        } else if (viewModel.availableCalendars.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("No calendars were found on this device.")
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_ADD_ACCOUNT)) }) {
                Text("Add an Account")
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            CalendarPicker(viewModel = viewModel)
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.editableShifts, key = { it.id }) { shift ->
                ShiftRow(
                    shift = shift,
                    onToggleIncluded = { included -> viewModel.setShiftIncluded(shift.id, included) },
                    onEdit = { editingShift = shift },
                )
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.addShiftsToCalendar()
                onImported()
            },
            enabled = calendarPermissionGranted &&
                viewModel.selectedCalendarId != null &&
                viewModel.editableShifts.any { it.included },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add to Calendar")
        }
    }

    editingShift?.let { shift ->
        ShiftEditDialog(
            shift = shift,
            onDismiss = { editingShift = null },
            onSave = { updated ->
                viewModel.updateShift(updated)
                editingShift = null
            },
        )
    }
}

@Composable
private fun CalendarPicker(viewModel: ScheduleImportViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selected = viewModel.availableCalendars.firstOrNull { it.id == viewModel.selectedCalendarId }

    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.let { "${it.displayName} (${it.accountName})" } ?: "Select a calendar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            viewModel.availableCalendars.forEach { calendar ->
                DropdownMenuItem(
                    text = { Text("${calendar.displayName} (${calendar.accountName})") },
                    onClick = {
                        viewModel.selectedCalendarId = calendar.id
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ShiftRow(
    shift: ParsedShift,
    onToggleIncluded: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val dateLabel = remember(shift.date) {
        val weekday = shift.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
        "$weekday ${shift.date.format(DATE_FORMATTER)}"
    }
    val timeLabel = when {
        shift.notScheduled -> "Not Scheduled"
        shift.startTime != null && shift.endTime != null ->
            "${shift.startTime.format(TIME_FORMATTER)} - ${shift.endTime.format(TIME_FORMATTER)}"
        else -> "Time not recognized - please edit"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!shift.notScheduled) {
            Checkbox(checked = shift.included, onCheckedChange = onToggleIncluded)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(dateLabel, style = MaterialTheme.typography.titleSmall)
            Text(timeLabel)
            if (!shift.notScheduled) {
                val details = listOfNotNull(shift.position, shift.storeNumber?.let { "Store #$it" })
                    .joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!shift.notScheduled) {
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
        }
    }
}
