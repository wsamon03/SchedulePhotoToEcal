package com.wsamon.schedulephototoecal.calendar

data class ImportResult(
    val added: Int,
    val skippedAsDuplicate: Int,
    val failed: Int,
)
