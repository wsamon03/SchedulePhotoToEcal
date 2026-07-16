package com.wsamon.schedulephototoecal.parser

import com.wsamon.schedulephototoecal.model.OcrTextLine
import java.time.LocalTime

data class RowFields(
    val notScheduled: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val storeNumber: String?,
    val rawHoursText: String?,
    val position: String?,
    val timeRegexFailed: Boolean,
)

/**
 * Extracts the shift fields (time range, store number, hours, position) out of the
 * OCR lines belonging to a single row body (badge lines already excluded).
 */
object RowFieldExtractor {

    private val NOT_SCHEDULED_REGEX = Regex("not\\s*scheduled", RegexOption.IGNORE_CASE)
    private val STORE_REGEX = Regex("store\\s*#?\\s*(\\d{3,6})", RegexOption.IGNORE_CASE)
    private val HOURS_REGEX = Regex("(\\d+(?:\\.\\d+)?)\\s*hours?", RegexOption.IGNORE_CASE)

    fun extract(rowLines: List<OcrTextLine>): RowFields {
        val texts = rowLines.map { it.text }

        if (texts.any { NOT_SCHEDULED_REGEX.containsMatchIn(it) }) {
            return RowFields(
                notScheduled = true,
                startTime = null,
                endTime = null,
                storeNumber = null,
                rawHoursText = null,
                position = null,
                timeRegexFailed = false,
            )
        }

        var timeLineIndex = -1
        var timeMatch: TimeRangeParser.TimeRangeMatch? = null
        for ((index, text) in texts.withIndex()) {
            val match = TimeRangeParser.parse(text)
            if (match != null) {
                timeMatch = match
                timeLineIndex = index
                break
            }
        }

        var storeLineIndex = -1
        var storeNumber: String? = null
        for ((index, text) in texts.withIndex()) {
            val match = STORE_REGEX.find(text)
            if (match != null) {
                storeNumber = match.groupValues[1]
                storeLineIndex = index
                break
            }
        }

        var hoursLineIndex = -1
        var rawHoursText: String? = null
        for ((index, text) in texts.withIndex()) {
            val match = HOURS_REGEX.find(text)
            if (match != null) {
                rawHoursText = match.groupValues[1]
                hoursLineIndex = index
                break
            }
        }

        val consumedIndices = setOf(timeLineIndex, storeLineIndex, hoursLineIndex)
        val position = texts
            .filterIndexed { index, _ -> index !in consumedIndices }
            .joinToString(" ") { it.trim() }
            .trim()
            .takeIf { it.isNotEmpty() }

        return RowFields(
            notScheduled = false,
            startTime = timeMatch?.start,
            endTime = timeMatch?.end,
            storeNumber = storeNumber,
            rawHoursText = rawHoursText,
            position = position,
            timeRegexFailed = timeMatch == null,
        )
    }
}
