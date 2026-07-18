package com.wsamon.schedulephototoecal.calendar

import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.reconcile.ShiftReconciliationAction

interface CalendarRepository {
    /** All calendars on the device, writable or not - callers decide which are usable. */
    fun listAllCalendars(): List<CalendarInfo>

    /** Read-only: figures out what re-importing would add/update/remove without writing anything. */
    fun planReconciliation(shifts: List<ParsedShift>, calendarId: Long): List<ShiftReconciliationAction>

    /** Executes a previously-computed plan, so a shown preview and the actual commit never diverge. */
    fun applyPlan(plan: List<ShiftReconciliationAction>, calendarId: Long): ImportResult
}
