package com.wsamon.schedulephototoecal.reconcile

import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.ZoneId

/** The calendar-event fields a [ParsedShift] should produce, for both Insert and Update. */
data class ShiftEventContent(
    val title: String,
    val description: String,
    val location: String?,
    val dtStart: Long,
    val dtEnd: Long,
    val timeZoneId: String,
)

object ShiftEventContentBuilder {

    /**
     * Written verbatim on every event this app creates, unchanged since the very first
     * version - also reused as the ownership signal for finding this app's own events on
     * re-import (see [com.wsamon.schedulephototoecal.calendar.AndroidCalendarRepository]),
     * so no separate/newly-visible marker is needed.
     */
    const val DESCRIPTION = "Imported from a Publix schedule photo via Schedule to Calendar"

    /** Null for `notScheduled` shifts or ones missing a parsed start/end time. */
    fun build(shift: ParsedShift, zoneId: ZoneId = ZoneId.systemDefault()): ShiftEventContent? {
        if (shift.notScheduled) return null
        val startTime = shift.startTime ?: return null
        val endTime = shift.endTime ?: return null

        val startInstant = shift.date.atTime(startTime).atZone(zoneId).toInstant()
        val endDate = if (endTime < startTime) shift.date.plusDays(1) else shift.date
        val endInstant = endDate.atTime(endTime).atZone(zoneId).toInstant()

        return ShiftEventContent(
            title = buildTitle(shift),
            description = DESCRIPTION,
            location = shift.storeNumber?.let { "Store #$it" },
            dtStart = startInstant.toEpochMilli(),
            dtEnd = endInstant.toEpochMilli(),
            timeZoneId = zoneId.id,
        )
    }

    private fun buildTitle(shift: ParsedShift): String {
        val position = shift.position ?: "Shift"
        val store = shift.storeNumber
        return if (store != null) "$position — Store #$store" else position
    }
}
