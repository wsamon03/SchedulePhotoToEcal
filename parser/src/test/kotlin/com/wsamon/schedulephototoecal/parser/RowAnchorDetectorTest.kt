package com.wsamon.schedulephototoecal.parser

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import org.junit.Test

class RowAnchorDetectorTest {

    @Test
    fun `detects all 7 anchors in the full week example in top-to-bottom order`() {
        val anchors = RowAnchorDetector.detect(ScheduleOcrFixtures.fullWeekExample())

        assertThat(anchors).hasSize(7)
        assertThat(anchors.map { it.dayOfWeek }).containsExactly(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ).inOrder()
        assertThat(anchors.map { it.ocrDayOfMonth }).containsExactly(11, 12, 13, 14, 15, 16, 17).inOrder()
    }

    @Test
    fun `header lines above the first row are never mistaken for anchors`() {
        val anchors = RowAnchorDetector.detect(ScheduleOcrFixtures.fullWeekExample())
        assertThat(anchors).hasSize(7)
    }

    @Test
    fun `pairs weekday and day-number tokens even when split further apart`() {
        val anchors = RowAnchorDetector.detect(ScheduleOcrFixtures.splitBadgeTokens())

        assertThat(anchors).hasSize(1)
        assertThat(anchors[0].dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(anchors[0].ocrDayOfMonth).isEqualTo(11)
    }

    @Test
    fun `partial week detects only the rows present`() {
        val anchors = RowAnchorDetector.detect(ScheduleOcrFixtures.partialWeek())
        assertThat(anchors).hasSize(3)
    }

    @Test
    fun `non-schedule photo yields zero anchors`() {
        val anchors = RowAnchorDetector.detect(ScheduleOcrFixtures.nonScheduleImage())
        assertThat(anchors).isEmpty()
    }
}
