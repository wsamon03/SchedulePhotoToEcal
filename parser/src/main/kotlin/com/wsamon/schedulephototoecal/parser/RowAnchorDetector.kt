package com.wsamon.schedulephototoecal.parser

import com.wsamon.schedulephototoecal.model.OcrTextLine
import java.time.DayOfWeek
import kotlin.math.abs

/**
 * A detected "day badge" for one schedule row (e.g. "Sat" + "11"), which anchors
 * where that row starts in the vertical layout of OCR text.
 */
data class RowAnchor(
    val dayOfWeek: DayOfWeek,
    val ocrDayOfMonth: Int,
    val top: Int,
    val bottom: Int,
    val consumedLines: Set<OcrTextLine>,
)

/**
 * [anchors] are the rows a full weekday+day-number badge was found for. [unpairedWeekdayTops]
 * are the vertical positions of weekday text (e.g. "Thu") that were recognized but never found
 * a day-number partner nearby - most often because the day-number glyph itself wasn't OCR'd at
 * all. These still mark a real row boundary even though no anchor could be built there, so a
 * caller can use them to stop a neighboring row's content from swallowing that row's stray text.
 */
data class RowDetectionResult(
    val anchors: List<RowAnchor>,
    val unpairedWeekdayTops: List<Int>,
)

/**
 * Finds the per-row weekday+day-number badges in a flat, unordered list of OCR lines.
 *
 * Row boundaries can't be found via generic vertical-gap clustering because each row is
 * itself a multi-line stack (badge, time range, position, store, hours) with no reliable
 * gap separating rows from the lines within them. The badge is the one structurally
 * guaranteed marker of "a new row started," so anchors are found directly instead.
 */
object RowAnchorDetector {

    private const val MAX_PAIRING_VERTICAL_GAP = 120
    private const val MIN_PAIRING_VERTICAL_GAP = -20
    private const val MAX_PAIRING_HORIZONTAL_OFFSET = 80

    private val WEEKDAY_ABBREVIATIONS: Map<String, DayOfWeek> = mapOf(
        "sun" to DayOfWeek.SUNDAY,
        "mon" to DayOfWeek.MONDAY,
        "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY,
        "thu" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY,
    )

    private val COMBINED_BADGE_REGEX = Regex(
        "^(sun|mon|tue|wed|thu|fri|sat)\\s+(\\d{1,2})$",
        RegexOption.IGNORE_CASE,
    )
    private val DAY_NUMBER_REGEX = Regex("^\\d{1,2}$")

    fun detect(lines: List<OcrTextLine>): RowDetectionResult {
        val anchors = mutableListOf<RowAnchor>()
        val unpairedWeekdayTops = mutableListOf<Int>()
        val usedDayNumberLines = mutableSetOf<OcrTextLine>()
        val dayNumberLines = lines.filter { DAY_NUMBER_REGEX.matches(it.text.trim()) }

        for (line in lines) {
            val trimmed = line.text.trim()

            val combinedMatch = COMBINED_BADGE_REGEX.matchEntire(trimmed)
            if (combinedMatch != null) {
                val dayOfWeek = WEEKDAY_ABBREVIATIONS.getValue(combinedMatch.groupValues[1].lowercase())
                val dayOfMonth = combinedMatch.groupValues[2].toInt()
                anchors += RowAnchor(dayOfWeek, dayOfMonth, line.top, line.bottom, setOf(line))
                continue
            }

            val dayOfWeek = WEEKDAY_ABBREVIATIONS[trimmed.lowercase()] ?: continue

            val partner = dayNumberLines
                .filter { it !in usedDayNumberLines }
                .filter { candidate ->
                    val verticalGap = candidate.top - line.bottom
                    val horizontalOffset = abs(candidate.left - line.left)
                    verticalGap in MIN_PAIRING_VERTICAL_GAP..MAX_PAIRING_VERTICAL_GAP &&
                        horizontalOffset <= MAX_PAIRING_HORIZONTAL_OFFSET
                }
                .minByOrNull { it.top }

            if (partner == null) {
                unpairedWeekdayTops += line.top
                continue
            }

            usedDayNumberLines += partner
            anchors += RowAnchor(
                dayOfWeek = dayOfWeek,
                ocrDayOfMonth = partner.text.trim().toInt(),
                top = line.top,
                bottom = partner.bottom,
                consumedLines = setOf(line, partner),
            )
        }

        return RowDetectionResult(anchors.sortedBy { it.top }, unpairedWeekdayTops.sorted())
    }
}
