package com.wsamon.schedulephototoecal.calendar

import com.wsamon.schedulephototoecal.model.ParsedShift

interface CalendarRepository {
    /** All calendars on the device, writable or not - callers decide which are usable. */
    fun listAllCalendars(): List<CalendarInfo>
    fun importShifts(shifts: List<ParsedShift>, calendarId: Long): ImportResult
}
