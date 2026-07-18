package com.wsamon.schedulephototoecal.reconcile

import com.wsamon.schedulephototoecal.model.ParsedShift

enum class NoOpReason { NOT_SCHEDULED_NO_EXISTING_EVENT, EXCLUDED, UNCHANGED, INVALID_MISSING_TIMES }

/** What [ScheduleReconciliationPlanner] decided to do for one [ParsedShift]. */
sealed interface ShiftReconciliationAction {
    val shift: ParsedShift

    data class Insert(override val shift: ParsedShift, val content: ShiftEventContent) : ShiftReconciliationAction

    data class Update(
        override val shift: ParsedShift,
        val existingEventId: Long,
        val content: ShiftEventContent,
    ) : ShiftReconciliationAction

    data class Delete(override val shift: ParsedShift, val existingEventId: Long) : ShiftReconciliationAction

    data class NoOp(override val shift: ParsedShift, val reason: NoOpReason) : ShiftReconciliationAction
}
