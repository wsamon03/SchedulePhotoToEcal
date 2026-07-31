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
import com.wsamon.schedulephototoecal.diagnostics.DiagnosticLogFormatter
import com.wsamon.schedulephototoecal.model.OcrTextLine
import com.wsamon.schedulephototoecal.model.ParseResult
import com.wsamon.schedulephototoecal.model.ParseStatus
import com.wsamon.schedulephototoecal.model.ParsedShift
import com.wsamon.schedulephototoecal.ocr.TextRecognitionService
import com.wsamon.schedulephototoecal.parser.ScheduleParser
import com.wsamon.schedulephototoecal.reconcile.ShiftReconciliationAction
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

    /** What re-importing would do, computed read-only ahead of the "Add to Calendar" tap. */
    var reconciliationPlan: List<ShiftReconciliationAction> by mutableStateOf(emptyList())
        private set

    var importResult: ImportResult? by mutableStateOf(null)
        private set

    /**
     * The raw OCR text from the most recent uncertain parse, held in memory only - never
     * logged or written to disk on its own. Cleared on a clean SUCCESS and on every new
     * import, so it never lingers longer than the situation that might need it.
     */
    private var lastOcrLines: List<OcrTextLine>? by mutableStateOf(null)

    /** True only when the last parse landed in an uncertain state and there's a log to show. */
    val diagnosticLogAvailable: Boolean
        get() = lastOcrLines != null && parseResult?.status != ParseStatus.SUCCESS

    /** Built lazily, only when the user explicitly asks to view or share the diagnostic log. */
    fun diagnosticLogText(): String = DiagnosticLogFormatter.format(lastOcrLines.orEmpty(), parseResult)

    fun startNewImport(uri: Uri) {
        capturedImageUri = uri
        parseResult = null
        editableShifts = emptyList()
        processingError = null
        reconciliationPlan = emptyList()
        importResult = null
        lastOcrLines = null
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
                lastOcrLines = if (result.status != ParseStatus.SUCCESS) lines else null
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

    /** Re-dates every shift by a fixed day offset, used to apply a corrected month/year guess. */
    fun shiftAllShiftDates(days: Long) {
        if (days == 0L) return
        editableShifts = editableShifts.map { it.copy(date = it.date.plusDays(days)) }
        parseResult = parseResult?.let { pr -> pr.copy(shifts = pr.shifts.map { it.copy(date = it.date.plusDays(days)) }) }
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

    fun refreshReconciliationPlan() {
        val calendarId = selectedCalendarId
        reconciliationPlan = if (calendarId != null) {
            calendarRepository.planReconciliation(editableShifts, calendarId)
        } else {
            emptyList()
        }
    }

    fun confirmImport() {
        val calendarId = selectedCalendarId ?: return
        importResult = calendarRepository.applyPlan(reconciliationPlan, calendarId)
    }

    fun startOver() {
        capturedImageUri = null
        parseResult = null
        editableShifts = emptyList()
        processingError = null
        reconciliationPlan = emptyList()
        importResult = null
        lastOcrLines = null
    }
}
