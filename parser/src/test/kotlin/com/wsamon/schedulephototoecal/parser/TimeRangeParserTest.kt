package com.wsamon.schedulephototoecal.parser

import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.Test

class TimeRangeParserTest {

    @Test
    fun `parses hyphen-separated range with periods and spaces`() {
        val match = TimeRangeParser.parse("4 p.m. - 11 p.m.")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(16, 0))
        assertThat(match.end).isEqualTo(LocalTime.of(23, 0))
    }

    @Test
    fun `parses range with minutes`() {
        val match = TimeRangeParser.parse("5:30 p.m. - 9:30 p.m.")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(17, 30))
        assertThat(match.end).isEqualTo(LocalTime.of(21, 30))
    }

    @Test
    fun `parses tightly packed range with no spaces`() {
        val match = TimeRangeParser.parse("9a.m.-5p.m.")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(9, 0))
        assertThat(match.end).isEqualTo(LocalTime.of(17, 0))
    }

    @Test
    fun `parses uppercase range with the word to as a separator`() {
        val match = TimeRangeParser.parse("9 A.M. TO 5 P.M.")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(9, 0))
        assertThat(match.end).isEqualTo(LocalTime.of(17, 0))
    }

    @Test
    fun `parses range with no periods and an em dash`() {
        val match = TimeRangeParser.parse("9:00am—5:00pm")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(9, 0))
        assertThat(match.end).isEqualTo(LocalTime.of(17, 0))
    }

    @Test
    fun `ignores stray warning icon noise before the time range`() {
        val match = TimeRangeParser.parse("⚠ 9 a.m. - 5 p.m.")
        assertThat(match).isNotNull()
        assertThat(match!!.start).isEqualTo(LocalTime.of(9, 0))
        assertThat(match.end).isEqualTo(LocalTime.of(17, 0))
    }

    @Test
    fun `12am and 12pm map to midnight and noon`() {
        val midnight = TimeRangeParser.parse("12 a.m. - 1 a.m.")
        assertThat(midnight!!.start).isEqualTo(LocalTime.of(0, 0))

        val noon = TimeRangeParser.parse("12 p.m. - 1 p.m.")
        assertThat(noon!!.start).isEqualTo(LocalTime.of(12, 0))
    }

    @Test
    fun `returns null for text with no time range`() {
        assertThat(TimeRangeParser.parse("Grocery Clerk")).isNull()
        assertThat(TimeRangeParser.parse("Not Scheduled")).isNull()
    }
}
