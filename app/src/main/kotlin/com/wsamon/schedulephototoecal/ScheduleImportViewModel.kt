package com.wsamon.schedulephototoecal

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wsamon.schedulephototoecal.calendar.AndroidCalendarRepository
import com.wsamon.schedulephototoecal.calendar.CalendarInfo
import com.wsamon.schedulephototoecal.calendar.CalendarPreferencesStore
import com.wsamon.schedulephototoecal.calendar.CalendarRepository
import com.wsamon.schedulephototoecal.calendar.ImportResult
import com.wsamon.schedulephototoecal.model.ParseResult
import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.ocr.TextRecognitionService
import com.wsamon.schedulephototoecal.parser.ScheduleParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

/**
 * Single source of truth for the whole Capture -> Processing -> Review -> Result flow,
 * shared across all four screens so state doesn't need to be threaded through Compose
 * Navigation arguments.
 */
class ScheduleImportViewModel(application: Application) : AndroidViewModel(application) {

    private val textRecognitionService = TextRecognitionService(application)
    private val calendarRepository: CalendarRepository = AndroidCalendarRepository(application)
    private val calendarPreferences = CalendarPreferencesStore(application)

    var capturedImageUri: Uri? by mutableStateOf(null)
        private set

    var parseResult: ParseResult? by mutableStateOf(null)
        private set

    var editableShifts: List<ParsedShift> by mutableStateOf(emptyList())
        private set

    var processingError: String? by mutableStateOf(null)
        private set

    /** Every calendar the device knows about, writable or not - backs the manage-calendars screen. */
    var allCalendars: List<CalendarInfo> by mutableStateOf(emptyList())
        private set

    /** Writable calendars the user hasn't hidden - backs the "Add to Calendar" picker. */
    var availableCalendars: List<CalendarInfo> by mutableStateOf(emptyList())
        private set

    var selectedCalendarId: Long? by mutableStateOf(null)

    var importResult: ImportResult? by mutableStateOf(null)
        private set

    fun startNewImport(uri: Uri) {
        capturedImageUri = uri
        parseResult = null
        editableShifts = emptyList()
        processingError = null
        importResult = null
    }

    fun processImage(onDone: () -> Unit) {
        val uri = capturedImageUri ?: return
        viewModelScope.launch {
            processingError = null
            try {
                val lines = textRecognitionService.recognize(uri)
                val result = ScheduleParser.parse(lines)
                parseResult = result
                editableShifts = result.shifts
                if (result.shifts.isEmpty()) {
                    processingError = result.warnings.firstOrNull()
                        ?: "We couldn't find a schedule in this photo. Please try another photo."
                }
            } catch (t: TimeoutCancellationException) {
                processingError = "This took too long to read. Please try again."
            } catch (t: CancellationException) {
                throw t
            } catch (t: Exception) {
                processingError = "Something went wrong reading this photo. Please try again."
            }
            onDone()
        }
    }

    fun updateShift(updated: ParsedShift) {
        editableShifts = editableShifts.map { if (it.id == updated.id) updated else it }
    }

    fun setShiftIncluded(shiftId: String, included: Boolean) {
        editableShifts = editableShifts.map { if (it.id == shiftId) it.copy(included = included) else it }
    }

    fun refreshCalendars() {
        allCalendars = calendarRepository.listAllCalendars()
        recomputeAvailableCalendars()
        if (selectedCalendarId == null || availableCalendars.none { it.id == selectedCalendarId }) {
            selectedCalendarId = availableCalendars.firstOrNull()?.id
        }
    }

    /** Toggles whether a writable calendar shows up in the "Add to Calendar" picker. */
    fun setCalendarShown(calendarId: Long, shown: Boolean) {
        val currentlyShown = calendarPreferences.getEnabledCalendarIds()
            ?: allCalendars.filter { it.isWritable }.map { it.id }.toSet()
        calendarPreferences.setEnabledCalendarIds(
            if (shown) currentlyShown + calendarId else currentlyShown - calendarId,
        )
        recomputeAvailableCalendars()
        if (selectedCalendarId != null && availableCalendars.none { it.id == selectedCalendarId }) {
            selectedCalendarId = availableCalendars.firstOrNull()?.id
        }
    }

    fun isCalendarShown(calendarId: Long): Boolean = availableCalendars.any { it.id == calendarId }

    private fun recomputeAvailableCalendars() {
        val shownIds = calendarPreferences.getEnabledCalendarIds()
        availableCalendars = allCalendars.filter { calendar ->
            calendar.isWritable && (shownIds == null || calendar.id in shownIds)
        }
    }

    fun addShiftsToCalendar() {
        val calendarId = selectedCalendarId ?: return
        importResult = calendarRepository.importShifts(editableShifts, calendarId)
    }

    fun startOver() {
        capturedImageUri = null
        parseResult = null
        editableShifts = emptyList()
        processingError = null
        importResult = null
    }
}
