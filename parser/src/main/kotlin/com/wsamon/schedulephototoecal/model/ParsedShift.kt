package com.wsamon.schedulephototoecal.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class ParseConfidence { HIGH, MEDIUM, LOW }

data class ParsedShift(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val notScheduled: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val position: String?,
    val storeNumber: String?,
    val rawOcrText: String,
    val confidence: ParseConfidence,
    val included: Boolean,
)
