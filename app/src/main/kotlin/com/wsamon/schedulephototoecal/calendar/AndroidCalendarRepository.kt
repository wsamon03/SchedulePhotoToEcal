package com.wsamon.schedulephototoecal.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.reconcile.ExistingEventSnapshot
import com.wsamon.schedulephototoecal.reconcile.ScheduleReconciliationPlanner
import com.wsamon.schedulephototoecal.reconcile.ShiftEventContent
import com.wsamon.schedulephototoecal.reconcile.ShiftEventContentBuilder
import com.wsamon.schedulephototoecal.reconcile.ShiftReconciliationAction
import java.time.Instant
import java.time.LocalDate
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

    override fun listAllCalendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )

        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val isPrimaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accessLevelIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                calendars += CalendarInfo(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(nameIndex) ?: "",
                    accountName = cursor.getString(accountNameIndex) ?: "",
                    accountType = cursor.getString(accountTypeIndex) ?: "",
                    isPrimary = isPrimaryIndex >= 0 && cursor.getInt(isPrimaryIndex) != 0,
                    isWritable = cursor.getInt(accessLevelIndex) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                )
            }
        }

        // Writable calendars first (only those are actually usable), then Google-account
        // calendars (the sync source eCalendar supports), then primary, then name. Every
        // calendar the device knows about is included - read-only ones are shown too so a
        // shared/subscribed calendar's exclusion from the picker is visible, not silent.
        return calendars.sortedWith(
            compareByDescending<CalendarInfo> { it.isWritable }
                .thenByDescending { it.accountType == "com.google" }
                .thenByDescending { it.isPrimary }
                .thenBy { it.displayName },
        )
    }

    override fun planReconciliation(shifts: List<ParsedShift>, calendarId: Long): List<ShiftReconciliationAction> {
        val zoneId = ZoneId.systemDefault()
        val existingEvents = findOwnedEvents(shifts, calendarId, zoneId)
        return ScheduleReconciliationPlanner.plan(shifts, existingEvents, zoneId)
    }

    override fun applyPlan(plan: List<ShiftReconciliationAction>, calendarId: Long): ImportResult {
        var added = 0
        var updated = 0
        var removed = 0
        var unchanged = 0
        var failed = 0

        for (action in plan) {
            try {
                when (action) {
                    is ShiftReconciliationAction.Insert -> {
                        val uri = context.contentResolver.insert(
                            CalendarContract.Events.CONTENT_URI,
                            contentValuesFor(action.content, calendarId),
                        )
                        if (uri != null) added++ else failed++
                    }
                    is ShiftReconciliationAction.Update -> {
                        val rows = context.contentResolver.update(
                            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, action.existingEventId),
                            contentValuesFor(action.content, calendarId = null),
                            null,
                            null,
                        )
                        if (rows > 0) updated++ else failed++
                    }
                    is ShiftReconciliationAction.Delete -> {
                        val rows = context.contentResolver.delete(
                            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, action.existingEventId),
                            null,
                            null,
                        )
                        if (rows > 0) removed++ else failed++
                    }
                    is ShiftReconciliationAction.NoOp -> unchanged++
                }
            } catch (e: SecurityException) {
                failed++
            } catch (e: IllegalArgumentException) {
                failed++
            }
        }

        return ImportResult(added = added, updated = updated, removed = removed, unchanged = unchanged, failed = failed)
    }

    private fun contentValuesFor(content: ShiftEventContent, calendarId: Long?): ContentValues =
        ContentValues().apply {
            calendarId?.let { put(CalendarContract.Events.CALENDAR_ID, it) }
            put(CalendarContract.Events.TITLE, content.title)
            put(CalendarContract.Events.DESCRIPTION, content.description)
            content.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            put(CalendarContract.Events.DTSTART, content.dtStart)
            put(CalendarContract.Events.DTEND, content.dtEnd)
            put(CalendarContract.Events.EVENT_TIMEZONE, content.timeZoneId)
            put(CalendarContract.Events.HAS_ALARM, 0)
        }

    /**
     * Finds events on [calendarId], within the shift list's date range, that carry this app's
     * fixed ownership description - written verbatim on every event this app has ever created,
     * so this recognizes events from any prior version too. Keyed by the [LocalDate] read off
     * each event's DTSTART (never encoded separately - see [ShiftEventContentBuilder]).
     */
    private fun findOwnedEvents(
        shifts: List<ParsedShift>,
        calendarId: Long,
        zoneId: ZoneId,
    ): Map<LocalDate, ExistingEventSnapshot> {
        if (shifts.isEmpty()) return emptyMap()

        val dates = shifts.map { it.date }
        val rangeStart = dates.min().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rangeEnd = dates.max().plusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ? AND " +
            "${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val selectionArgs = arrayOf(
            calendarId.toString(),
            rangeStart.toString(),
            rangeEnd.toString(),
            "%${ShiftEventContentBuilder.DESCRIPTION}%",
        )

        val result = mutableMapOf<LocalDate, ExistingEventSnapshot>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val dtStartIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val dtEndIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)

            while (cursor.moveToNext()) {
                val dtStart = cursor.getLong(dtStartIndex)
                val date = Instant.ofEpochMilli(dtStart).atZone(zoneId).toLocalDate()
                // If more than one owned event somehow exists for the same date, deterministically
                // keep the first and leave the rest alone rather than guessing which is current.
                if (date in result) continue
                result[date] = ExistingEventSnapshot(
                    eventId = cursor.getLong(idIndex),
                    title = cursor.getString(titleIndex) ?: "",
                    dtStart = dtStart,
                    dtEnd = cursor.getLong(dtEndIndex),
                    location = cursor.getString(locationIndex),
                )
            }
        }
        return result
    }
}
