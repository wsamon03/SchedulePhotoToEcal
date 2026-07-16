package com.wsamon.schedulephototoecal.calendar

import android.content.Context

/**
 * Persists which calendars the user has chosen to show in the "Add to Calendar" picker.
 * Absence of a stored preference (the common case, before the user has ever opened the
 * manage-calendars screen) means "show every writable calendar" - nothing is hidden by
 * default.
 */
class CalendarPreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Null means no preference has been set yet - callers should default to "all shown". */
    fun getEnabledCalendarIds(): Set<Long>? {
        if (!prefs.contains(KEY_ENABLED_IDS)) return null
        return prefs.getStringSet(KEY_ENABLED_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    fun setEnabledCalendarIds(ids: Set<Long>) {
        prefs.edit()
            .putStringSet(KEY_ENABLED_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "calendar_prefs"
        const val KEY_ENABLED_IDS = "enabled_calendar_ids"
    }
}
