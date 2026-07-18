package com.wsamon.schedulephototoecal.ui.calendars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.calendar.CalendarInfo
import com.wsamon.schedulephototoecal.ui.theme.Spacing

/**
 * Lets the user browse every calendar the device knows about and choose which writable ones
 * show up in the Review screen's "Add to Calendar" picker. Read-only calendars (shared or
 * subscribed) are shown too, disabled, so their absence from the picker is visible rather
 * than silently confusing - a calendar that isn't synced to the device at all still won't
 * appear here, since this reads from the same CalendarContract query as the picker.
 */
@Composable
fun ManageCalendarsScreen(
    viewModel: ScheduleImportViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.refreshCalendars()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Calendars") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            Text(
                "Choose which calendars show up in the \"Add to Calendar\" picker. Read-only " +
                    "calendars (shared or subscribed) can't be used and are listed for reference only. " +
                    "If a calendar you created is missing entirely, make sure it's turned on for sync " +
                    "in your calendar app's account settings.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider()

            if (viewModel.allCalendars.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("No calendars were found on this device.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(vertical = Spacing.sm),
                ) {
                    items(viewModel.allCalendars, key = { it.id }) { calendar ->
                        CalendarRow(
                            calendar = calendar,
                            shown = viewModel.isCalendarShown(calendar.id),
                            onShownChange = { shown -> viewModel.setCalendarShown(calendar.id, shown) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(
    calendar: CalendarInfo,
    shown: Boolean,
    onShownChange: (Boolean) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = shown,
                onCheckedChange = onShownChange,
                enabled = calendar.isWritable,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(calendar.displayName, style = MaterialTheme.typography.titleSmall)
                Text(calendar.accountName, style = MaterialTheme.typography.bodySmall)
                if (!calendar.isWritable) {
                    Text(
                        "Read-only - can't be used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
