package com.wsamon.schedulephototoecal.parser

import com.wsamon.schedulephototoecal.model.OcrTextLine
import com.wsamon.schedulephototoecal.model.ParseConfidence
import com.wsamon.schedulephototoecal.model.ParseResult
import com.wsamon.schedulephototoecal.model.ParseStatus
import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import kotlin.math.abs

private const val EXPECTED_ROW_COUNT = 7
private const val HOURS_CROSS_CHECK_TOLERANCE = 0.5
private const val MAX_PLAUSIBLE_UNPAID_BREAK_HOURS = 2.0

/**
 * Warning added when [ScheduleParser.parse] falls back to guessing the date. Exposed so callers
 * (e.g. the Review screen) can filter it out once the guess has already been confirmed elsewhere.
 */
const val DATE_GUESSED_WARNING =
    "We couldn't read this week's date from the photo, so we guessed it from the days shown. " +
        "Please confirm it's correct."

/**
 * Turns a flat, unordered list of OCR text lines from a Publix weekly schedule
 * screenshot into a structured list of [ParsedShift]s.
 */
object ScheduleParser {

    fun parse(lines: List<OcrTextLine>, clock: Clock = Clock.systemDefaultZone()): ParseResult {
        val anchors = RowAnchorDetector.detect(lines)
        if (anchors.isEmpty()) {
            return ParseResult(
                shifts = emptyList(),
                status = ParseStatus.NO_SCHEDULE_DETECTED,
                warnings = listOf("No schedule rows were found in this photo."),
            )
        }

        val ocrDayOfMonthSequence = anchors.map { it.ocrDayOfMonth }
        val headerLines = lines.filter { it.top < anchors.first().top }
        val headerDate = HeaderDateParser.findHeaderDate(headerLines)
        val dateWasGuessed = headerDate == null
        val startDate = headerDate
            ?: ScheduleDateGuesser.guessStartDate(ocrDayOfMonthSequence, LocalDate.now(clock))
            ?: return ParseResult(
                shifts = emptyList(),
                status = ParseStatus.NO_HEADER_DATE,
                warnings = listOf("Could not read the week's date from this photo."),
                ocrDayOfMonthSequence = ocrDayOfMonthSequence,
            )

        val warnings = mutableListOf<String>()
        if (dateWasGuessed) {
            warnings += DATE_GUESSED_WARNING
        }
        if (anchors.size < EXPECTED_ROW_COUNT) {
            warnings += "Only ${anchors.size} of $EXPECTED_ROW_COUNT days were detected in this photo."
        }

        val consumedAnchorLines = anchors.flatMap { it.consumedLines }.toSet()

        val shifts = anchors.mapIndexed { index, anchor ->
            val rowTop = anchor.top
            val rowBottom = if (index < anchors.size - 1) anchors[index + 1].top else Int.MAX_VALUE
            val rowBodyLines = lines
                .filter { it !in consumedAnchorLines }
                .filter { it.verticalCenter() >= rowTop && it.verticalCenter() < rowBottom }
                .sortedBy { it.top }

            val date = startDate.plusDays(index.toLong())
            val dateMismatch = anchor.dayOfWeek != date.dayOfWeek || anchor.ocrDayOfMonth != date.dayOfMonth
            if (dateMismatch) {
                warnings += "Row for $date did not match its detected weekday/day-of-month badge; please verify."
            }

            val fields = RowFieldExtractor.extract(rowBodyLines)
            val rawRowText = rowBodyLines.joinToString(" ") { it.text }
            buildShift(date, fields, dateMismatch, rawRowText)
        }

        val status = when {
            dateWasGuessed -> ParseStatus.DATE_GUESSED
            anchors.size < EXPECTED_ROW_COUNT -> ParseStatus.PARTIAL
            else -> ParseStatus.SUCCESS
        }
        return ParseResult(shifts, status, warnings, ocrDayOfMonthSequence)
    }

    private fun buildShift(
        date: LocalDate,
        fields: RowFields,
        dateMismatch: Boolean,
        rawRowText: String,
    ): ParsedShift {
        if (fields.notScheduled) {
            return ParsedShift(
                date = date,
                notScheduled = true,
                startTime = null,
                endTime = null,
                position = null,
                storeNumber = null,
                rawOcrText = rawRowText,
                confidence = if (dateMismatch) ParseConfidence.LOW else ParseConfidence.HIGH,
                included = false,
            )
        }

        val confidence = when {
            dateMismatch || fields.timeRegexFailed -> ParseConfidence.LOW
            fields.position.isNullOrBlank() || fields.storeNumber.isNullOrBlank() -> ParseConfidence.MEDIUM
            !hoursCrossCheckPasses(fields) -> ParseConfidence.MEDIUM
            else -> ParseConfidence.HIGH
        }

        return ParsedShift(
            date = date,
            notScheduled = false,
            startTime = fields.startTime,
            endTime = fields.endTime,
            position = fields.position,
            storeNumber = fields.storeNumber,
            rawOcrText = rawRowText,
            confidence = confidence,
            included = fields.startTime != null && fields.endTime != null,
        )
    }

    /**
     * Publix's reported "hours" is paid time, not raw shift span: shifts with an unpaid
     * meal break routinely show fewer hours than `end - start` (e.g. a 9am-5pm shift
     * commonly shows "7 hours" for a 1-hour unpaid lunch). So the raw span must never be
     * *less* than the reported hours, but it's expected to run up to ~2 hours *more* for a
     * typical unpaid break; only a bigger gap (or a span shorter than reported) suggests a
     * genuine OCR misread.
     */
    private fun hoursCrossCheckPasses(fields: RowFields): Boolean {
        val expectedHours = fields.rawHoursText?.toDoubleOrNull() ?: return true
        val start = fields.startTime ?: return true
        val end = fields.endTime ?: return true
        var minutes = Duration.between(start, end).toMinutes()
        if (minutes < 0) minutes += 24 * 60
        val actualHours = minutes / 60.0
        val breakHours = actualHours - expectedHours
        return breakHours >= -HOURS_CROSS_CHECK_TOLERANCE && breakHours <= MAX_PLAUSIBLE_UNPAID_BREAK_HOURS
    }
}
