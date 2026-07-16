package com.wsamon.schedulephototoecal.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.wsamon.schedulephototoecal.model.OcrTextLine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val RECOGNITION_TIMEOUT_MS = 15_000L

/**
 * Runs on-device ML Kit text recognition on a captured/picked image and flattens the
 * result to a plain, Android-independent line list. Block-level grouping from ML Kit is
 * deliberately discarded here since block boundaries don't reliably match the visual rows
 * of a Publix schedule screenshot; [com.wsamon.schedulephototoecal.parser.ScheduleParser]
 * does its own row reconstruction from this flat list.
 */
class TextRecognitionService(private val context: Context) {

    suspend fun recognize(uri: Uri): List<OcrTextLine> = withTimeout(RECOGNITION_TIMEOUT_MS) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, uri)
        val text = recognizer.process(image).await()
        text.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrTextLine(line.text, box.left, box.top, box.right, box.bottom)
            }
            .sortedBy { it.top }
    }
}
