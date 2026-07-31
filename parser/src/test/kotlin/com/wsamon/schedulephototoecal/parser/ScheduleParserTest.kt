package com.wsamon.schedulephototoecal.parser

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.ParseConfidence
import com.wsamon.schedulephototoecal.model.ParseStatus
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Test

class ScheduleParserTest {

    private fun fixedClock(date: LocalDate): Clock =
        Clock.fixed(date.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

    @Test
    fun `full week example parses all 7 rows with high confidence`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.fullWeekExample())

        assertThat(result.status).isEqualTo(ParseStatus.SUCCESS)
        assertThat(result.shifts).hasSize(7)
        assertThat(result.shifts.all { it.confidence == ParseConfidence.HIGH }).isTrue()

        val notScheduled = result.shifts[0]
        assertThat(notScheduled.date).isEqualTo(LocalDate.of(2026, 7, 11))
        assertThat(notScheduled.notScheduled).isTrue()
        assertThat(notScheduled.included).isFalse()

        val sunday = result.shifts[1]
        assertThat(sunday.date).isEqualTo(LocalDate.of(2026, 7, 12))
        assertThat(sunday.startTime).isEqualTo(LocalTime.of(16, 0))
        assertThat(sunday.endTime).isEqualTo(LocalTime.of(23, 0))
        assertThat(sunday.position).isEqualTo("Grocery Clerk")
        assertThat(sunday.storeNumber).isEqualTo("1861")
        assertThat(sunday.included).isTrue()

        val monday = result.shifts[2]
        assertThat(monday.date).isEqualTo(LocalDate.of(2026, 7, 13))
        assertThat(monday.startTime).isEqualTo(LocalTime.of(17, 30))
        assertThat(monday.endTime).isEqualTo(LocalTime.of(21, 30))
        assertThat(monday.position).isEqualTo("Liquor Clerk")
        assertThat(monday.storeNumber).isEqualTo("1309")

        val tuesday = result.shifts[3]
        assertThat(tuesday.date).isEqualTo(LocalDate.of(2026, 7, 14))
        assertThat(tuesday.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(tuesday.endTime).isEqualTo(LocalTime.of(17, 0))

        val wednesday = result.shifts[4]
        assertThat(wednesday.date).isEqualTo(LocalDate.of(2026, 7, 15))
        assertThat(wednesday.startTime).isEqualTo(LocalTime.of(14, 0))
        assertThat(wednesday.endTime).isEqualTo(LocalTime.of(23, 0))

        val thursday = result.shifts[5]
        assertThat(thursday.date).isEqualTo(LocalDate.of(2026, 7, 16))
        assertThat(thursday.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(thursday.endTime).isEqualTo(LocalTime.of(17, 0))

        val friday = result.shifts[6]
        assertThat(friday.date).isEqualTo(LocalDate.of(2026, 7, 17))
        assertThat(friday.startTime).isEqualTo(LocalTime.of(17, 30))
        assertThat(friday.endTime).isEqualTo(LocalTime.of(21, 30))
        assertThat(friday.position).isEqualTo("Liquor Clerk")
        assertThat(friday.storeNumber).isEqualTo("1309")
    }

    @Test
    fun `warning icon noise on a time line does not break parsing`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.fullWeekExampleWithWarningIconNoise())

        assertThat(result.status).isEqualTo(ParseStatus.SUCCESS)
        val tuesday = result.shifts[3]
        assertThat(tuesday.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(tuesday.endTime).isEqualTo(LocalTime.of(17, 0))
        assertThat(tuesday.confidence).isEqualTo(ParseConfidence.HIGH)
    }

    @Test
    fun `overnight shift keeps chronological start and end time`() {
        // Fixture is a single isolated row (not a realistic full 7-row photo), so it's
        // correctly reported as PARTIAL - only the parsed time values are under test here.
        // The other 6 days of the week are padded in as not-found-in-photo placeholders.
        val result = ScheduleParser.parse(ScheduleOcrFixtures.overnightShiftRow())

        assertThat(result.status).isEqualTo(ParseStatus.PARTIAL)
        assertThat(result.shifts).hasSize(7)
        val shift = result.shifts[0]
        assertThat(shift.startTime).isEqualTo(LocalTime.of(22, 0))
        assertThat(shift.endTime).isEqualTo(LocalTime.of(6, 0))
        assertThat(shift.confidence).isEqualTo(ParseConfidence.HIGH)
        assertThat(result.shifts.drop(1).all { it.notFoundInPhoto }).isTrue()
    }

    @Test
    fun `missing header date falls back to guessing the month from the day badges`() {
        val result = ScheduleParser.parse(
            ScheduleOcrFixtures.missingHeaderDate(),
            clock = fixedClock(LocalDate.of(2026, 7, 17)),
        )

        assertThat(result.status).isEqualTo(ParseStatus.DATE_GUESSED)
        assertThat(result.shifts).hasSize(7)
        // Day 11 has already passed in July (today is fixed to the 17th), so the nearest
        // future occurrence of "day 11" is in August, not the current month.
        assertThat(result.shifts[0].date).isEqualTo(LocalDate.of(2026, 8, 11))
        assertThat(result.warnings.any { it.contains("guessed", ignoreCase = true) }).isTrue()
        assertThat(result.ocrDayOfMonthSequence).containsExactly(11)
    }

    @Test
    fun `a day-of-month sequence no month can reproduce is still a hard failure`() {
        val result = ScheduleParser.parse(
            ScheduleOcrFixtures.missingHeaderDateWithImpossibleSequence(),
            clock = fixedClock(LocalDate.of(2026, 7, 17)),
        )

        assertThat(result.status).isEqualTo(ParseStatus.NO_HEADER_DATE)
        assertThat(result.shifts).isEmpty()
    }

    @Test
    fun `guessed dates correctly roll over a month boundary`() {
        val result = ScheduleParser.parse(
            ScheduleOcrFixtures.missingHeaderDateWithRollover(),
            clock = fixedClock(LocalDate.of(2026, 7, 17)),
        )

        assertThat(result.status).isEqualTo(ParseStatus.DATE_GUESSED)
        assertThat(result.shifts).hasSize(7)
        val real = result.shifts.filterNot { it.notFoundInPhoto }
        assertThat(real.map { it.date }).containsExactly(
            LocalDate.of(2026, 9, 30),
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
        ).inOrder()
        assertThat(real.all { it.confidence == ParseConfidence.HIGH }).isTrue()
        assertThat(result.warnings.any { it.contains("guessed", ignoreCase = true) }).isTrue()
    }

    @Test
    fun `partial week is reported as PARTIAL but still parses detected rows`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.partialWeek())

        assertThat(result.status).isEqualTo(ParseStatus.PARTIAL)
        assertThat(result.shifts).hasSize(7)
        assertThat(result.shifts.count { it.notFoundInPhoto }).isEqualTo(4)
        assertThat(result.warnings.any { it.contains("Only 3 of 7") }).isTrue()
    }

    @Test
    fun `non-schedule photo yields no rows detected`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.nonScheduleImage())

        assertThat(result.status).isEqualTo(ParseStatus.NO_SCHEDULE_DETECTED)
        assertThat(result.shifts).isEmpty()
    }

    @Test
    fun `month and year rollover is handled purely by sequential day increment`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.monthRolloverWeek())

        assertThat(result.status).isEqualTo(ParseStatus.SUCCESS)
        assertThat(result.shifts.map { it.date }).containsExactly(
            LocalDate.of(2026, 7, 29),
            LocalDate.of(2026, 7, 30),
            LocalDate.of(2026, 7, 31),
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 4),
        ).inOrder()
        assertThat(result.shifts.all { it.confidence == ParseConfidence.HIGH }).isTrue()
    }

    @Test
    fun `split badge tokens still anchor a row correctly`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.splitBadgeTokens())

        assertThat(result.status).isEqualTo(ParseStatus.PARTIAL)
        assertThat(result.shifts).hasSize(7)
        assertThat(result.shifts[0].date).isEqualTo(LocalDate.of(2026, 7, 11))
        assertThat(result.shifts[0].notScheduled).isTrue()
        assertThat(result.shifts[0].notFoundInPhoto).isFalse()
    }

    @Test
    fun `a day whose number was never OCR'd does not shift dates or leak into the row before it`() {
        val result = ScheduleParser.parse(ScheduleOcrFixtures.weekWithMissingMiddleDay())

        assertThat(result.status).isEqualTo(ParseStatus.PARTIAL)
        assertThat(result.warnings.any { it.contains("Only 6 of 7") }).isTrue()
        assertThat(result.shifts).hasSize(7)
        assertThat(result.shifts.map { it.date }).containsExactly(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 4),
            LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 8, 6),
            LocalDate.of(2026, 8, 7),
        ).inOrder()

        val thursday = result.shifts.single { it.date == LocalDate.of(2026, 8, 6) }
        assertThat(thursday.notFoundInPhoto).isTrue()
        assertThat(thursday.notScheduled).isTrue()
        assertThat(thursday.included).isFalse()

        // Wednesday - the row right before the gap - must keep its own real shift, not
        // absorb Thursday's stray "Thu"/"Not Scheduled" text and get misread as not scheduled.
        val wednesday = result.shifts.single { it.date == LocalDate.of(2026, 8, 5) }
        assertThat(wednesday.notFoundInPhoto).isFalse()
        assertThat(wednesday.notScheduled).isFalse()
        assertThat(wednesday.startTime).isEqualTo(LocalTime.of(14, 0))
        assertThat(wednesday.position).isEqualTo("Grocery Clerk")
        assertThat(wednesday.storeNumber).isEqualTo("1309")
        assertThat(wednesday.confidence).isEqualTo(ParseConfidence.HIGH)

        // Friday - the day right after the gap - must keep its own correct date too.
        val friday = result.shifts.single { it.date == LocalDate.of(2026, 8, 7) }
        assertThat(friday.notFoundInPhoto).isFalse()
        assertThat(friday.startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(friday.confidence).isEqualTo(ParseConfidence.HIGH)
    }
}
