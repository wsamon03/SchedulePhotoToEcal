package com.wsamon.schedulephototoecal.model

enum class ParseStatus { SUCCESS, PARTIAL, NO_SCHEDULE_DETECTED, NO_HEADER_DATE, DATE_GUESSED }

data class ParseResult(
    val shifts: List<ParsedShift>,
    val status: ParseStatus,
    val warnings: List<String>,
    val ocrDayOfMonthSequence: List<Int> = emptyList(),
)
