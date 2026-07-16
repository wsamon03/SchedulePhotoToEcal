package com.wsamon.schedulephototoecal.calendar

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean,
    val isWritable: Boolean,
)
