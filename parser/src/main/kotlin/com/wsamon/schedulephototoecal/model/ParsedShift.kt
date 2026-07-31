package com.wsamon.schedulephototoecal.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class ParseConfidence { HIGH, MEDIUM, LOW }

data class ParsedShift(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val notScheduled: Boolean,
    /** True for a day the photo simply didn't show a badge for at all (distinct from a real "Not Scheduled" day). */
    val notFoundInPhoto: Boolean = false,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val position: String?,
    val storeNumber: String?,
    val rawOcrText: String,
    val confidence: ParseConfidence,
    val included: Boolean,
)
