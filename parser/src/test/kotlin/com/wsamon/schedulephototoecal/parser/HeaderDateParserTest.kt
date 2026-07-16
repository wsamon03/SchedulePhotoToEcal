package com.wsamon.schedulephototoecal.parser

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.OcrTextLine
import java.time.LocalDate
import org.junit.Test

class HeaderDateParserTest {

    @Test
    fun `finds a simple M-d-yyyy header date`() {
        val lines = listOf(OcrTextLine("7/11/2026", left = 0, top = 0, right = 100, bottom = 20))
        assertThat(HeaderDateParser.findHeaderDate(lines)).isEqualTo(LocalDate.of(2026, 7, 11))
    }

    @Test
    fun `returns null when no header lines contain a date`() {
        val lines = listOf(OcrTextLine("Net hours: 36", left = 0, top = 0, right = 100, bottom = 20))
        assertThat(HeaderDateParser.findHeaderDate(lines)).isNull()
    }

    @Test
    fun `returns null for an impossible date rather than throwing`() {
        val lines = listOf(OcrTextLine("13/45/2026", left = 0, top = 0, right = 100, bottom = 20))
        assertThat(HeaderDateParser.findHeaderDate(lines)).isNull()
    }

    @Test
    fun `month rollover across the full week is handled by sequential increment, not the parser`() {
        // The header date itself is a single day; rollover across the week happens in
        // ScheduleParser via plusDays, verified end-to-end in ScheduleParserTest. This
        // test only confirms the header date nearest a month boundary parses correctly.
        val lines = listOf(OcrTextLine("7/29/2026", left = 0, top = 0, right = 100, bottom = 20))
        assertThat(HeaderDateParser.findHeaderDate(lines)).isEqualTo(LocalDate.of(2026, 7, 29))
    }
}
