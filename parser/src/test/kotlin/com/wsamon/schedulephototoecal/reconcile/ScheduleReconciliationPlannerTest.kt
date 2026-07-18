package com.wsamon.schedulephototoecal.reconcile

import com.google.common.truth.Truth.assertThat
import com.wsamon.schedulephototoecal.model.ParseConfidence
import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Test

class ScheduleReconciliationPlannerTest {

    private val zone = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 7, 12)

    private fun notScheduledShift() = ParsedShift(
        date = date,
        notScheduled = true,
        startTime = null,
        endTime = null,
        position = null,
        storeNumber = null,
        rawOcrText = "Not Scheduled",
        confidence = ParseConfidence.HIGH,
        included = false,
    )

    private fun scheduledShift(
        included: Boolean = true,
        startTime: LocalTime = LocalTime.of(16, 0),
        endTime: LocalTime = LocalTime.of(23, 0),
        position: String = "Grocery Clerk",
        storeNumber: String = "1309",
    ) = ParsedShift(
        date = date,
        notScheduled = false,
        startTime = startTime,
        endTime = endTime,
        position = position,
        storeNumber = storeNumber,
        rawOcrText = "",
        confidence = ParseConfidence.HIGH,
        included = included,
    )

    private fun snapshotFor(shift: ParsedShift, eventId: Long = 1L): ExistingEventSnapshot {
        val content = ShiftEventContentBuilder.build(shift, zone)!!
        return ExistingEventSnapshot(
            eventId = eventId,
            title = content.title,
            dtStart = content.dtStart,
            dtEnd = content.dtEnd,
            location = content.location,
        )
    }

    @Test
    fun `not scheduled with no existing event is a no-op`() {
        val actions = ScheduleReconciliationPlanner.plan(listOf(notScheduledShift()), emptyMap(), zone)
        assertThat(actions).hasSize(1)
        val action = actions[0] as ShiftReconciliationAction.NoOp
        assertThat(action.reason).isEqualTo(NoOpReason.NOT_SCHEDULED_NO_EXISTING_EVENT)
    }

    @Test
    fun `not scheduled with an existing event deletes it`() {
        val shift = notScheduledShift()
        val existing = ExistingEventSnapshot(eventId = 42L, title = "Grocery Clerk", dtStart = 1L, dtEnd = 2L, location = null)
        val actions = ScheduleReconciliationPlanner.plan(listOf(shift), mapOf(date to existing), zone)
        val action = actions[0] as ShiftReconciliationAction.Delete
        assertThat(action.existingEventId).isEqualTo(42L)
    }

    @Test
    fun `scheduled and excluded with no existing event is a no-op`() {
        val actions = ScheduleReconciliationPlanner.plan(listOf(scheduledShift(included = false)), emptyMap(), zone)
        val action = actions[0] as ShiftReconciliationAction.NoOp
        assertThat(action.reason).isEqualTo(NoOpReason.EXCLUDED)
    }

    @Test
    fun `scheduled and included with no existing event inserts`() {
        val shift = scheduledShift()
        val actions = ScheduleReconciliationPlanner.plan(listOf(shift), emptyMap(), zone)
        val action = actions[0] as ShiftReconciliationAction.Insert
        assertThat(action.content.title).isEqualTo("Grocery Clerk — Store #1309")
    }

    @Test
    fun `scheduled and excluded leaves an existing event untouched`() {
        val shift = scheduledShift(included = false)
        val existing = snapshotFor(scheduledShift(included = true))
        val actions = ScheduleReconciliationPlanner.plan(listOf(shift), mapOf(date to existing), zone)
        val action = actions[0] as ShiftReconciliationAction.NoOp
        assertThat(action.reason).isEqualTo(NoOpReason.EXCLUDED)
    }

    @Test
    fun `scheduled included and unchanged is a no-op`() {
        val shift = scheduledShift()
        val existing = snapshotFor(shift)
        val actions = ScheduleReconciliationPlanner.plan(listOf(shift), mapOf(date to existing), zone)
        val action = actions[0] as ShiftReconciliationAction.NoOp
        assertThat(action.reason).isEqualTo(NoOpReason.UNCHANGED)
    }

    @Test
    fun `scheduled included and time changed updates in place`() {
        val original = scheduledShift()
        val existing = snapshotFor(original, eventId = 7L)
        val changed = scheduledShift(startTime = LocalTime.of(17, 0))
        val actions = ScheduleReconciliationPlanner.plan(listOf(changed), mapOf(date to existing), zone)
        val action = actions[0] as ShiftReconciliationAction.Update
        assertThat(action.existingEventId).isEqualTo(7L)
        assertThat(action.content.dtStart).isNotEqualTo(existing.dtStart)
    }

    @Test
    fun `multiple changed fields still yield exactly one update`() {
        val original = scheduledShift()
        val existing = snapshotFor(original, eventId = 9L)
        val changed = scheduledShift(
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            position = "Liquor Clerk",
            storeNumber = "1861",
        )
        val actions = ScheduleReconciliationPlanner.plan(listOf(changed), mapOf(date to existing), zone)
        assertThat(actions).hasSize(1)
        assertThat(actions[0]).isInstanceOf(ShiftReconciliationAction.Update::class.java)
    }

    @Test
    fun `a date with no map entry is treated the same as no existing event`() {
        val shift = scheduledShift()
        val actions = ScheduleReconciliationPlanner.plan(
            listOf(shift),
            mapOf(date.plusDays(1) to snapshotFor(shift)),
            zone,
        )
        assertThat(actions[0]).isInstanceOf(ShiftReconciliationAction.Insert::class.java)
    }

    @Test
    fun `missing start or end time is a defensive no-op, never inserted blindly`() {
        val shift = scheduledShift().copy(startTime = null)
        val actions = ScheduleReconciliationPlanner.plan(listOf(shift), emptyMap(), zone)
        val action = actions[0] as ShiftReconciliationAction.NoOp
        assertThat(action.reason).isEqualTo(NoOpReason.INVALID_MISSING_TIMES)
    }
}
