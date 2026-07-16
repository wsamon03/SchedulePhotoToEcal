package com.wsamon.schedulephototoecal.parser

import java.time.LocalTime

/**
 * Parses a shift time range (e.g. "4 p.m. - 11 p.m.", "9a.m.-5p.m.", "9:30 AM to 5:30 PM")
 * out of a single line of noisy OCR text.
 */
object TimeRangeParser {

    data class TimeRangeMatch(val start: LocalTime, val end: LocalTime)

    private const val TIME_PATTERN = "(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?\\s*m\\.?"
    private val RANGE_REGEX = Regex(
        "$TIME_PATTERN\\s*(?:-|–|—|to)\\s*$TIME_PATTERN",
        RegexOption.IGNORE_CASE,
    )
    private val STRIP_UNKNOWN_CHARS = Regex("[^a-z0-9:.\\-–— ]")

    fun parse(rawLine: String): TimeRangeMatch? {
        val normalized = normalize(rawLine)
        val match = RANGE_REGEX.find(normalized) ?: return null
        val groups = match.groupValues
        val start = toLocalTime(hourStr = groups[1], minuteStr = groups[2], ampm = groups[3]) ?: return null
        val end = toLocalTime(hourStr = groups[4], minuteStr = groups[5], ampm = groups[6]) ?: return null
        return TimeRangeMatch(start, end)
    }

    private fun normalize(raw: String): String = STRIP_UNKNOWN_CHARS.replace(raw.lowercase(), "")

    private fun toLocalTime(hourStr: String, minuteStr: String, ampm: String): LocalTime? {
        val hour = hourStr.toIntOrNull() ?: return null
        if (hour !in 1..12) return null
        val minute = if (minuteStr.isEmpty()) 0 else minuteStr.toIntOrNull() ?: return null
        if (minute !in 0..59) return null
        val isPm = ampm.equals("p", ignoreCase = true)
        val hour24 = when {
            hour == 12 && !isPm -> 0
            hour == 12 && isPm -> 12
            isPm -> hour + 12
            else -> hour
        }
        return LocalTime.of(hour24, minute)
    }
}
