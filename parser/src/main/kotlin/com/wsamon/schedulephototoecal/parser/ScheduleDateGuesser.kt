package com.wsamon.schedulephototoecal.parser

import java.time.LocalDate
import java.time.YearMonth

private const val MONTHS_BACK = 1L
private const val MONTHS_FORWARD = 13L

/**
 * Infers which calendar month/year an ordered sequence of OCR'd day-of-month badge
 * values (RowAnchorDetector output, in row order) most plausibly belongs to, for use
 * when no header date is legible. Returns the resulting start date (day-of-month ==
 * sequence.first()) such that sequentially incrementing it by one calendar day per
 * subsequent row reproduces [sequence] exactly, or null if no candidate month
 * reproduces the sequence while ending on or after [today].
 */
object ScheduleDateGuesser {

    fun guessStartDate(sequence: List<Int>, today: LocalDate): LocalDate? {
        if (sequence.isEmpty()) return null
        val anchorMonth = YearMonth.from(today)
        return (-MONTHS_BACK..MONTHS_FORWARD)
            .mapNotNull { offset ->
                val start = runCatching {
                    anchorMonth.plusMonths(offset).atDay(sequence.first())
                }.getOrNull() ?: return@mapNotNull null
                if (!reproducesSequence(start, sequence)) return@mapNotNull null
                val end = start.plusDays((sequence.size - 1).toLong())
                if (end.isBefore(today)) return@mapNotNull null
                start
            }
            .minOrNull()
    }

    private fun reproducesSequence(start: LocalDate, sequence: List<Int>): Boolean =
        sequence.indices.all { i -> start.plusDays(i.toLong()).dayOfMonth == sequence[i] }
}
