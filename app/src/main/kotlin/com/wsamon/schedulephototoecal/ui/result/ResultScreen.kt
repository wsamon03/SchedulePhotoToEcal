package com.wsamon.schedulephototoecal.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.ui.theme.Spacing

@Composable
fun ResultScreen(
    viewModel: ScheduleImportViewModel,
    onImportAnotherWeek: () -> Unit,
) {
    val result = viewModel.importResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Done!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.md))

        if (result == null) {
            Text("No import result available.", textAlign = TextAlign.Center)
        } else {
            if (result.added > 0) {
                Text("${result.added} shift(s) added.", textAlign = TextAlign.Center)
            }
            if (result.updated > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("${result.updated} shift(s) updated.", textAlign = TextAlign.Center)
            }
            if (result.removed > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("${result.removed} shift(s) removed.", textAlign = TextAlign.Center)
            }
            if (result.unchanged > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("${result.unchanged} shift(s) already up to date.", textAlign = TextAlign.Center)
            }
            if (result.failed > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "${result.failed} shift(s) could not be updated.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            if (result.added == 0 && result.updated == 0 && result.removed == 0 &&
                result.unchanged == 0 && result.failed == 0
            ) {
                Text("No changes were made.", textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        Button(onClick = onImportAnotherWeek) {
            Text("Import Another Week")
        }
    }
}
