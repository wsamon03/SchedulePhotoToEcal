package com.wsamon.schedulephototoecal.reconcile

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.ParseConfidence
import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Test

class ShiftEventContentBuilderTest {

    private val zone = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 7, 12)

    private fun scheduledShift(
        startTime: LocalTime? = LocalTime.of(16, 0),
        endTime: LocalTime? = LocalTime.of(23, 0),
        position: String? = "Grocery Clerk",
        storeNumber: String? = "1309",
    ) = ParsedShift(
        date = date,
        notScheduled = false,
        startTime = startTime,
        endTime = endTime,
        position = position,
        storeNumber = storeNumber,
        rawOcrText = "",
        confidence = ParseConfidence.HIGH,
        included = true,
    )

    @Test
    fun `title includes store number when present`() {
        val content = ShiftEventContentBuilder.build(scheduledShift(), zone)
        assertThat(content!!.title).isEqualTo("Grocery Clerk — Store #1309")
    }

    @Test
    fun `title omits store number when absent`() {
        val content = ShiftEventContentBuilder.build(scheduledShift(storeNumber = null), zone)
        assertThat(content!!.title).isEqualTo("Grocery Clerk")
        assertThat(content.location).isNull()
    }

    @Test
    fun `title falls back to Shift when position is missing`() {
        val content = ShiftEventContentBuilder.build(scheduledShift(position = null), zone)
        assertThat(content!!.title).isEqualTo("Shift — Store #1309")
    }

    @Test
    fun `description is always the fixed ownership sentence`() {
        val content = ShiftEventContentBuilder.build(scheduledShift(), zone)
        assertThat(content!!.description)
            .isEqualTo("Imported from a Publix schedule photo via Schedule to Calendar")
    }

    @Test
    fun `dtStart and dtEnd land on the same day for a normal shift`() {
        val content = ShiftEventContentBuilder.build(scheduledShift(), zone)!!
        assertThat(content.dtStart).isEqualTo(date.atTime(16, 0).atZone(zone).toInstant().toEpochMilli())
        assertThat(content.dtEnd).isEqualTo(date.atTime(23, 0).atZone(zone).toInstant().toEpochMilli())
    }

    @Test
    fun `overnight shift rolls dtEnd to the next day`() {
        val content = ShiftEventContentBuilder.build(
            scheduledShift(startTime = LocalTime.of(22, 0), endTime = LocalTime.of(6, 0)),
            zone,
        )!!
        assertThat(content.dtStart).isEqualTo(date.atTime(22, 0).atZone(zone).toInstant().toEpochMilli())
        assertThat(content.dtEnd)
            .isEqualTo(date.plusDays(1).atTime(6, 0).atZone(zone).toInstant().toEpochMilli())
    }

    @Test
    fun `returns null for a not-scheduled shift`() {
        val shift = scheduledShift().copy(notScheduled = true, startTime = null, endTime = null, included = false)
        assertThat(ShiftEventContentBuilder.build(shift, zone)).isNull()
    }

    @Test
    fun `returns null when start or end time is missing`() {
        assertThat(ShiftEventContentBuilder.build(scheduledShift(startTime = null), zone)).isNull()
        assertThat(ShiftEventContentBuilder.build(scheduledShift(endTime = null), zone)).isNull()
    }
}
