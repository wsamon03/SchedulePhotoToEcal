package com.wsamon.schedulephototoecal.diagnostics

import com.wsamon.schedulephototoecal.model.OcrTextLine
import com.wsamon.schedulephototoecal.model.ParseResult

/**
 * Formats the raw OCR text ML Kit produced for a photo, alongside the parse outcome, into a
 * human-readable dump. Only meant to be built and shown when a caller explicitly asks for it
 * (e.g. after an uncertain parse) - this formatter itself has no side effects, logs nothing,
 * and writes nothing to disk.
 */
object DiagnosticLogFormatter {

    fun format(lines: List<OcrTextLine>, result: ParseResult?): String = buildString {
        appendLine("Schedule to Calendar diagnostic log")
        appendLine("Parse status: ${result?.status}")
        result?.warnings?.forEach { appendLine("Warning: $it") }
        appendLine()
        appendLine("Raw OCR text blocks, top to bottom (text @ left,top,right,bottom):")
        if (lines.isEmpty()) {
            appendLine("(none)")
        } else {
            lines.sortedBy { it.top }.forEach { line ->
                appendLine("\"${line.text}\" @ ${line.left},${line.top},${line.right},${line.bottom}")
            }
        }
    }
}
