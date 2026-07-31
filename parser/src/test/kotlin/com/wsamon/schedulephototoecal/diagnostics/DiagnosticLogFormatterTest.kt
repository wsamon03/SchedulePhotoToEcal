package com.wsamon.schedulephototoecal.diagnostics

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.OcrTextLine
import com.wsamon.schedulephototoecal.model.ParseResult
import com.wsamon.schedulephototoecal.model.ParseStatus
import org.junit.Test

class DiagnosticLogFormatterTest {

    @Test
    fun `includes status, warnings, and every raw line sorted top to bottom`() {
        val lines = listOf(
            OcrTextLine("Fri", left = 0, top = 100, right = 30, bottom = 120),
            OcrTextLine("Net hours: 36", left = 0, top = 0, right = 100, bottom = 20),
        )
        val result = ParseResult(
            shifts = emptyList(),
            status = ParseStatus.PARTIAL,
            warnings = listOf("Only 6 of 7 days were detected in this photo."),
        )

        val log = DiagnosticLogFormatter.format(lines, result)

        assertThat(log).contains("Parse status: PARTIAL")
        assertThat(log).contains("Only 6 of 7 days were detected in this photo.")
        val netHoursIndex = log.indexOf("Net hours: 36")
        val friIndex = log.indexOf("\"Fri\"")
        assertThat(netHoursIndex).isGreaterThan(-1)
        assertThat(friIndex).isGreaterThan(netHoursIndex)
    }

    @Test
    fun `handles a null result and empty lines without throwing`() {
        val log = DiagnosticLogFormatter.format(emptyList(), null)

        assertThat(log).contains("Parse status: null")
        assertThat(log).contains("(none)")
    }
}
