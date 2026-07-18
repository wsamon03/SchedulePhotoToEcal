package com.wsamon.schedulephototoecal.reconcile

/**
 * A calendar event the Android layer has already determined this app owns (matched the
 * ownership description), keyed elsewhere by the [java.time.LocalDate] read off [dtStart].
 * Deliberately Android-independent so [ScheduleReconciliationPlanner] stays pure/testable.
 */
data class ExistingEventSnapshot(
    val eventId: Long,
    val title: String,
    val dtStart: Long,
    val dtEnd: Long,
    val location: String?,
)
