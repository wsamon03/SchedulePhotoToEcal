package com.wsamon.schedulephototoecal.parser

import com.wsamon.schedulephototoecal.model.OcrTextLine
import java.time.LocalDate

/**
 * Finds the week's anchor date (e.g. "7/11/2026") among the OCR lines above the
 * first detected row. This date belongs to the topmost visible row; every other
 * row's date is derived from it by sequential day increment.
 */
object HeaderDateParser {

    private val DATE_REGEX = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})")

    fun findHeaderDate(headerLines: List<OcrTextLine>): LocalDate? {
        for (line in headerLines) {
            val match = DATE_REGEX.find(line.text) ?: continue
            val month = match.groupValues[1].toIntOrNull() ?: continue
            val day = match.groupValues[2].toIntOrNull() ?: continue
            val year = match.groupValues[3].toIntOrNull() ?: continue
            val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: continue
            return date
        }
        return null
    }
}
