package com.wsamon.schedulephototoecal.parser

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class ScheduleDateGuesserTest {

    @Test
    fun `same-month days that are still upcoming guess the current month`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(24, 25, 26), today))
            .isEqualTo(LocalDate.of(2026, 7, 24))
    }

    @Test
    fun `days that have already passed this month guess the next month`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(1, 2, 3), today))
            .isEqualTo(LocalDate.of(2026, 8, 1))
    }

    @Test
    fun `a 30-to-1 rollover only matches a 30-day month, skipping past 31-day months`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(30, 1, 2), today))
            .isEqualTo(LocalDate.of(2026, 9, 30))
    }

    @Test
    fun `leap year February boundary is respected`() {
        val today = LocalDate.of(2028, 1, 25)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(28, 29, 1), today))
            .isEqualTo(LocalDate.of(2028, 2, 28))
    }

    @Test
    fun `current month still wins when its range has not fully elapsed`() {
        val today = LocalDate.of(2026, 7, 5)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(10, 11, 12), today))
            .isEqualTo(LocalDate.of(2026, 7, 10))
    }

    @Test
    fun `a range ending exactly today is not treated as past`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(17), today))
            .isEqualTo(LocalDate.of(2026, 7, 17))
    }

    @Test
    fun `a sequence that skips a day is impossible for any month`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(1, 3), today)).isNull()
    }

    @Test
    fun `a sequence that repeats a day is impossible for any month`() {
        val today = LocalDate.of(2026, 7, 17)
        assertThat(ScheduleDateGuesser.guessStartDate(listOf(10, 10), today)).isNull()
    }

    @Test
    fun `an empty sequence has nothing to guess`() {
        assertThat(ScheduleDateGuesser.guessStartDate(emptyList(), LocalDate.of(2026, 7, 17))).isNull()
    }
}
