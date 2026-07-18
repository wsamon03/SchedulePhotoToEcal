package com.wsamon.schedulephototoecal.ui.confirmdate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.ui.theme.Spacing
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Shown when [ScheduleParser] couldn't read an explicit date from the photo and guessed the
 * month/year from the day-of-month badges instead. The guess must be confirmed - or corrected -
 * before any shift can reach Review/the calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDateScreen(
    viewModel: ScheduleImportViewModel,
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
) {
    val shifts = viewModel.editableShifts
    if (shifts.isEmpty()) {
        LaunchedEffect(Unit) { onCancel() }
        return
    }

    val originalFirstDate = remember { shifts.first().date }
    var selectedYearMonth by remember { mutableStateOf(YearMonth.from(originalFirstDate)) }
    val candidateDate = remember(selectedYearMonth) {
        runCatching { selectedYearMonth.atDay(originalFirstDate.dayOfMonth) }.getOrNull()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Confirm schedule dates") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text("Please confirm or adjust the month and year of the first date shown in the schedule")
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                "Guessed range: ${shifts.first().date} - ${shifts.last().date}",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            MonthYearPicker(selected = selectedYearMonth, onSelectedChange = { selectedYearMonth = it })

            if (candidateDate == null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Day ${originalFirstDate.dayOfMonth} doesn't exist in that month.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            Button(
                onClick = {
                    val target = candidateDate ?: return@Button
                    val days = ChronoUnit.DAYS.between(originalFirstDate, target)
                    viewModel.shiftAllShiftDates(days)
                    onConfirmed()
                },
                enabled = candidateDate != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirm")
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel Import")
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun MonthYearPicker(selected: YearMonth, onSelectedChange: (YearMonth) -> Unit) {
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    val years = remember(selected) { (selected.year - 1)..(selected.year + 2) }

    Row {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { monthExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.month.getDisplayName(TextStyle.FULL, Locale.US))
            }
            DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                Month.entries.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.getDisplayName(TextStyle.FULL, Locale.US)) },
                        onClick = {
                            onSelectedChange(YearMonth.of(selected.year, month))
                            monthExpanded = false
                        },
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
            OutlinedButton(onClick = { yearExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.year.toString())
            }
            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                years.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onSelectedChange(YearMonth.of(year, selected.month))
                            yearExpanded = false
                        },
                    )
                }
            }
        }
    }
}
