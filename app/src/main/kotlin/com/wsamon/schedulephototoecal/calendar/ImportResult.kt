package com.wsamon.schedulephototoecal.calendar

data class ImportResult(
    val added: Int,
    val updated: Int,
    val removed: Int,
    val unchanged: Int,
    val failed: Int,
)
