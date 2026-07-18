package com.wsamon.schedulephototoecal.ui.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.model.ParseStatus
import com.wsamon.schedulephototoecal.ui.theme.Spacing

@Composable
fun ProcessingScreen(
    viewModel: ScheduleImportViewModel,
    onParsed: () -> Unit,
    onRetake: () -> Unit,
) {
    LaunchedEffect(viewModel.capturedImageUri) {
        viewModel.processImage {
            when (viewModel.parseResult?.status) {
                ParseStatus.SUCCESS, ParseStatus.PARTIAL -> onParsed()
                else -> Unit
            }
        }
    }

    val error = viewModel.processingError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Button(onClick = onRetake) {
                Text("Retake / Choose a Different Photo")
            }
        } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(Spacing.md))
            Text("Reading your schedule...")
        }
    }
}
