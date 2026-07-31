package com.wsamon.schedulephototoecal.parser

import com.wsamon.schedulephototoecal.model.OcrTextLine

/**
 * Hand-built OCR line fixtures reproducing realistic ML Kit output for Publix
 * schedule screenshots, including the exact content of the reference example
 * image (header date 7/11/2026, Sat 11 through Fri 17).
 */
object ScheduleOcrFixtures {

    private const val ROW_HEIGHT = 200
    private const val ROW_0_TOP = 400
    private const val BADGE_LEFT = 20
    private const val CONTENT_LEFT = 100
    private const val CONTENT_RIGHT = 500

    private fun headerLines(dateText: String): List<OcrTextLine> = listOf(
        OcrTextLine("Net hours: 36", left = 20, top = 250, right = 300, bottom = 280),
        OcrTextLine(dateText, left = 400, top = 250, right = 550, bottom = 280),
        OcrTextLine("Scheduled  Shift Pickup  Requests", left = 20, top = 320, right = 500, bottom = 350),
    )

    /** Weekday + day-of-month badge as two separate OCR lines, matching real ML Kit output. */
    private fun badge(weekday: String, dayOfMonth: Int, bandTop: Int): List<OcrTextLine> = listOf(
        OcrTextLine(weekday, left = BADGE_LEFT, top = bandTop, right = BADGE_LEFT + 50, bottom = bandTop + 20),
        OcrTextLine(
            dayOfMonth.toString(),
            left = BADGE_LEFT + 5,
            top = bandTop + 25,
            right = BADGE_LEFT + 35,
            bottom = bandTop + 50,
        ),
    )

    private fun contentLine(text: String, top: Int): OcrTextLine =
        OcrTextLine(text, left = CONTENT_LEFT, top = top, right = CONTENT_RIGHT, bottom = top + 25)

    private fun notScheduledRow(weekday: String, dayOfMonth: Int, rowIndex: Int): List<OcrTextLine> {
        val bandTop = ROW_0_TOP + rowIndex * ROW_HEIGHT
        return badge(weekday, dayOfMonth, bandTop) + contentLine("Not Scheduled", bandTop + 15)
    }

    /**
     * A "Not Scheduled" row where the weekday text was recognized but its day-number glyph
     * was not OCR'd at all (no digit line emitted for it) - the exact failure mode seen in a
     * real diagnostic log, distinct from the badge going entirely undetected.
     */
    private fun notScheduledRowWithUnreadableDayNumber(weekday: String, rowIndex: Int): List<OcrTextLine> {
        val bandTop = ROW_0_TOP + rowIndex * ROW_HEIGHT
        return listOf(
            OcrTextLine(weekday, left = BADGE_LEFT, top = bandTop, right = BADGE_LEFT + 50, bottom = bandTop + 20),
        ) + contentLine("Not Scheduled", bandTop + 45)
    }

    private fun scheduledRow(
        weekday: String,
        dayOfMonth: Int,
        rowIndex: Int,
        timeRangeText: String,
        position: String,
        storeNumber: String,
        hours: String,
    ): List<OcrTextLine> {
        val bandTop = ROW_0_TOP + rowIndex * ROW_HEIGHT
        return badge(weekday, dayOfMonth, bandTop) +
            contentLine(timeRangeText, bandTop + 10) +
            contentLine(position, bandTop + 45) +
            contentLine("Store #$storeNumber", bandTop + 80) +
            contentLine("$hours hours", bandTop + 115)
    }

    /** The exact 7 rows from the reference example screenshot (header date 7/11/2026). */
    fun fullWeekExample(): List<OcrTextLine> =
        headerLines("7/11/2026") +
            notScheduledRow("Sat", 11, rowIndex = 0) +
            scheduledRow("Sun", 12, 1, "4 p.m. - 11 p.m.", "Grocery Clerk", "1861", "6") +
            scheduledRow("Mon", 13, 2, "5:30 p.m. - 9:30 p.m.", "Liquor Clerk", "1309", "4") +
            scheduledRow("Tue", 14, 3, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "7") +
            scheduledRow("Wed", 15, 4, "2 p.m. - 11 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Thu", 16, 5, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "7") +
            scheduledRow("Fri", 17, 6, "5:30 p.m. - 9:30 p.m.", "Liquor Clerk", "1309", "4")

    /** Same as [fullWeekExample] but the Tuesday time line carries stray warning-icon OCR noise. */
    fun fullWeekExampleWithWarningIconNoise(): List<OcrTextLine> {
        val lines = fullWeekExample().toMutableList()
        val index = lines.indexOfFirst { it.text == "9 a.m. - 5 p.m." && it.top == ROW_0_TOP + 3 * ROW_HEIGHT + 10 }
        lines[index] = lines[index].copy(text = "⚠ 9 a.m. - 5 p.m.")
        return lines
    }

    fun overnightShiftRow(): List<OcrTextLine> =
        headerLines("3/2/2026") + scheduledRow("Mon", 2, 0, "10 p.m. - 6 a.m.", "Overnight Stocker", "1309", "8")

    fun missingHeaderDate(): List<OcrTextLine> =
        listOf(OcrTextLine("Net hours: 36", left = 20, top = 250, right = 300, bottom = 280)) +
            notScheduledRow("Sat", 11, rowIndex = 0)

    /** No header date, and the day-of-month badges skip a day - impossible for any month to reproduce. */
    fun missingHeaderDateWithImpossibleSequence(): List<OcrTextLine> =
        listOf(OcrTextLine("Net hours: 36", left = 20, top = 250, right = 300, bottom = 280)) +
            notScheduledRow("Sat", 11, rowIndex = 0) +
            notScheduledRow("Mon", 13, rowIndex = 1)

    /** No header date; day-of-month badges 30, 1, 2 straddle a month boundary the guesser must resolve. */
    fun missingHeaderDateWithRollover(): List<OcrTextLine> =
        listOf(OcrTextLine("Net hours: 36", left = 20, top = 250, right = 300, bottom = 280)) +
            scheduledRow("Wed", 30, 0, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Thu", 1, 1, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Fri", 2, 2, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8")

    fun noisyTimeVariants(): List<OcrTextLine> =
        headerLines("7/11/2026") +
            scheduledRow("Sat", 11, 0, "9a.m.-5p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Sun", 12, 1, "9 A.M. TO 5 P.M.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Mon", 13, 2, "9:00am—5:00pm", "Grocery Clerk", "1309", "8")

    /** Only 3 of 7 rows detected (badge partially unreadable/cropped for the rest). */
    fun partialWeek(): List<OcrTextLine> =
        headerLines("7/11/2026") +
            notScheduledRow("Sat", 11, rowIndex = 0) +
            scheduledRow("Sun", 12, 1, "4 p.m. - 11 p.m.", "Grocery Clerk", "1861", "6") +
            scheduledRow("Mon", 13, 2, "5:30 p.m. - 9:30 p.m.", "Liquor Clerk", "1309", "4")

    /**
     * Reproduces a reported bug, taken from a real diagnostic log: a full 7-day week where
     * Thursday's weekday text ("Thu") was OCR'd fine, but its day-number glyph ("6") was not
     * recognized at all - no digit line exists for it anywhere in the OCR output. Every other
     * day must still land on its own correct date, and Wednesday's real shift (the row right
     * before the gap) must not absorb Thursday's stray "Thu"/"Not Scheduled" text into its own
     * row body. Values mirror the real schedule screenshot (header date 8/1/2026, Sat 1 through
     * Fri 7).
     */
    fun weekWithMissingMiddleDay(): List<OcrTextLine> =
        headerLines("8/1/2026") +
            notScheduledRow("Sat", 1, rowIndex = 0) +
            scheduledRow("Sun", 2, 1, "8 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Mon", 3, 2, "5:30 p.m. - 9:30 p.m.", "Liquor Clerk", "1309", "4") +
            scheduledRow("Tue", 4, 3, "8 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Wed", 5, 4, "2 p.m. - 11 p.m.", "Grocery Clerk", "1309", "8") +
            notScheduledRowWithUnreadableDayNumber("Thu", rowIndex = 5) +
            scheduledRow("Fri", 7, 6, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "7")

    fun nonScheduleImage(): List<OcrTextLine> = listOf(
        OcrTextLine("Welcome to Publix", left = 20, top = 100, right = 400, bottom = 130),
        OcrTextLine("where shopping is a pleasure", left = 20, top = 150, right = 500, bottom = 180),
    )

    /** Header date near a month boundary, to verify month/year rollover via plain day increment. */
    fun monthRolloverWeek(): List<OcrTextLine> =
        headerLines("7/29/2026") +
            scheduledRow("Wed", 29, 0, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Thu", 30, 1, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Fri", 31, 2, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Sat", 1, 3, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Sun", 2, 4, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Mon", 3, 5, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8") +
            scheduledRow("Tue", 4, 6, "9 a.m. - 5 p.m.", "Grocery Clerk", "1309", "8")

    /** Weekday/day-of-month badge tokens split further apart than usual, still pairable. */
    fun splitBadgeTokens(): List<OcrTextLine> =
        headerLines("7/11/2026") +
            listOf(
                OcrTextLine("Sat", left = 20, top = 400, right = 70, bottom = 420),
                OcrTextLine("11", left = 22, top = 460, right = 52, bottom = 485),
                contentLine("Not Scheduled", 415),
            )
}
