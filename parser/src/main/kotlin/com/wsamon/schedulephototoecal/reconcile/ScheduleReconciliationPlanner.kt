package com.wsamon.schedulephototoecal.reconcile

import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.LocalDate
import java.time.ZoneId

/**
 * Decides, per shift, whether re-importing should insert a new event, update an existing
 * one in place, delete a stale one, or do nothing - pure function of the shifts and whatever
 * events the caller has already determined this app owns for those dates. Never re-derives
 * ownership itself; ownership determination stays entirely in the Android I/O layer.
 */
object ScheduleReconciliationPlanner {

    fun plan(
        shifts: List<ParsedShift>,
        existingEvents: Map<LocalDate, ExistingEventSnapshot>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ShiftReconciliationAction> = shifts.map { shift -> planOne(shift, existingEvents[shift.date], zoneId) }

    private fun planOne(
        shift: ParsedShift,
        existing: ExistingEventSnapshot?,
        zoneId: ZoneId,
    ): ShiftReconciliationAction {
        if (shift.notScheduled) {
            return if (existing != null) {
                ShiftReconciliationAction.Delete(shift, existing.eventId)
            } else {
                ShiftReconciliationAction.NoOp(shift, NoOpReason.NOT_SCHEDULED_NO_EXISTING_EVENT)
            }
        }

        if (!shift.included) {
            return ShiftReconciliationAction.NoOp(shift, NoOpReason.EXCLUDED)
        }

        val content = ShiftEventContentBuilder.build(shift, zoneId)
            ?: return ShiftReconciliationAction.NoOp(shift, NoOpReason.INVALID_MISSING_TIMES)

        if (existing == null) {
            return ShiftReconciliationAction.Insert(shift, content)
        }

        val unchanged = existing.title == content.title &&
            existing.dtStart == content.dtStart &&
            existing.dtEnd == content.dtEnd &&
            existing.location == content.location

        return if (unchanged) {
            ShiftReconciliationAction.NoOp(shift, NoOpReason.UNCHANGED)
        } else {
            ShiftReconciliationAction.Update(shift, existing.eventId, content)
        }
    }
}
