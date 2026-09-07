package com.heckmannch.birthdaybuddy.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

interface Clock {
    fun currentTimeMillis(): Long
    fun nowLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        java.time.Instant.ofEpochMilli(currentTimeMillis()).atZone(zoneId).toLocalDate()
    fun nowLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        java.time.Instant.ofEpochMilli(currentTimeMillis()).atZone(zoneId).toLocalDateTime()
}

class SystemClock @Inject constructor() : Clock {
    override fun currentTimeMillis() = System.currentTimeMillis()
    override fun nowLocalDate(zoneId: ZoneId): LocalDate = LocalDate.now(zoneId)
    override fun nowLocalDateTime(zoneId: ZoneId): LocalDateTime = LocalDateTime.now(zoneId)
}

