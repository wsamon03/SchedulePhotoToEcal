package com.wsamon.schedulephototoecal.calendar

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.wsamon.schedulephototoecal.model.ParsedShift
import java.time.ZoneId

/**
 * Writes shifts as real calendar events via Android's CalendarContract provider.
 *
 * There is no public developer API for the user's "eCalendar - Daily Planner" app
 * (com.fujia.ecalendar) - it syncs by connecting Google/Apple/Outlook cloud accounts into
 * its own UI rather than reading on-device local calendars directly. Writing through
 * CalendarContract to a calendar/account the user picks (e.g. their Google account) is the
 * only reliable way in; this class never hardcodes a specific provider.
 */
class AndroidCalendarRepository(private val context: Context) : CalendarRepository {

    override fun listWritableCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val isPrimaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

            while (cursor.moveToNext()) {
                calendars += CalendarInfo(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(nameIndex) ?: "",
                    accountName = cursor.getString(accountNameIndex) ?: "",
                    accountType = cursor.getString(accountTypeIndex) ?: "",
                    isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) != 0,
                )
            }
        }

        // Google-account calendars sorted first since that's the sync source eCalendar
        // supports, but every writable calendar is shown - never hardcoded to just Google.
        return calendars.sortedWith(
            compareByDescending<CalendarInfo> { it.accountType == "com.google" }
                .thenByDescending { it.isPrimary }
                .thenBy { it.displayName },
        )
    }

    override fun importShifts(shifts: List<ParsedShift>, calendarId: Long): ImportResult {
        var added = 0
        var skipped = 0
        var failed = 0
        val zoneId = ZoneId.systemDefault()

        for (shift in shifts) {
            if (shift.notScheduled || !shift.included) continue
            val startTime = shift.startTime ?: continue
            val endTime = shift.endTime ?: continue

            val startInstant = shift.date.atTime(startTime).atZone(zoneId).toInstant()
            val endDate = if (endTime < startTime) shift.date.plusDays(1) else shift.date
            val endInstant = endDate.atTime(endTime).atZone(zoneId).toInstant()
            val dtStart = startInstant.toEpochMilli()
            val dtEnd = endInstant.toEpochMilli()
            val title = buildTitle(shift)

            if (eventAlreadyExists(calendarId, dtStart, dtEnd, title)) {
                skipped++
                continue
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(
                    CalendarContract.Events.DESCRIPTION,
                    "Imported from a Publix schedule photo via Schedule to Calendar",
                )
                shift.storeNumber?.let { put(CalendarContract.Events.EVENT_LOCATION, "Store #$it") }
                put(CalendarContract.Events.DTSTART, dtStart)
                put(CalendarContract.Events.DTEND, dtEnd)
                put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
                put(CalendarContract.Events.HAS_ALARM, 0)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) added++ else failed++
        }

        return ImportResult(added = added, skippedAsDuplicate = skipped, failed = failed)
    }

    private fun buildTitle(shift: ParsedShift): String {
        val position = shift.position ?: "Shift"
        val store = shift.storeNumber
        return if (store != null) "$position — Store #$store" else position
    }

    private fun eventAlreadyExists(calendarId: Long, dtStart: Long, dtEnd: Long, title: String): Boolean {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
            "${CalendarContract.Events.DTSTART} = ? AND " +
            "${CalendarContract.Events.DTEND} = ? AND " +
            "${CalendarContract.Events.TITLE} = ?"
        val selectionArgs = arrayOf(calendarId.toString(), dtStart.toString(), dtEnd.toString(), title)

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }
}
