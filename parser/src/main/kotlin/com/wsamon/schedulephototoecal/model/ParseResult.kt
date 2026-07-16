package com.wsamon.schedulephototoecal.model

enum class ParseStatus { SUCCESS, PARTIAL, NO_SCHEDULE_DETECTED, NO_HEADER_DATE }

data class ParseResult(
    val shifts: List<ParsedShift>,
    val status: ParseStatus,
    val warnings: List<String>,
)
