package com.wsamon.schedulephototoecal.calendar

import com.wsamon.schedulephototoecal.model.ParsedShift

interface CalendarRepository {
    fun listWritableCalendars(): List<CalendarInfo>
    fun importShifts(shifts: List<ParsedShift>, calendarId: Long): ImportResult
}
