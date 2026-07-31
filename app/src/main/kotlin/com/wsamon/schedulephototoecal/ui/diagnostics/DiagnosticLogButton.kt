package com.wsamon.schedulephototoecal.ui.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wsamon.schedulephototoecal.ScheduleImportViewModel

/**
 * Offers to view or share the raw OCR text for the most recent photo, but only when that
 * photo's parse landed in an uncertain state (see [ScheduleImportViewModel.diagnosticLogAvailable]).
 * Nothing is shown, shared, or written anywhere unless the user explicitly taps through -
 * this composable renders nothing at all for a confident (SUCCESS) parse.
 */
@Composable
fun DiagnosticLogButton(viewModel: ScheduleImportViewModel) {
    if (!viewModel.diagnosticLogAvailable) return
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    TextButton(onClick = { showDialog = true }) {
        Text("View Diagnostic Log")
    }

    if (showDialog) {
        val logText = remember { viewModel.diagnosticLogText() }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Diagnostic Log") },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(logText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, logText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Save or share diagnostic log"))
                }) {
                    Text("Save / Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
