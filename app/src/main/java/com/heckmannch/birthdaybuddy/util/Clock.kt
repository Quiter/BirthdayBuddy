package com.heckmannch.birthdaybuddy.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Abstraction for wall-clock time and time-zone queries to enable deterministic unit testing.
 */
interface Clock {
    fun currentTimeMillis(): Long

    /**
     * Current [ZoneId] used when converting epoch instants to local/zoned date-time values.
     * Defaults to [ZoneId.systemDefault] so runtime time-zone changes are reflected automatically.
     */
    val zone: ZoneId
        get() = ZoneId.systemDefault()

    fun nowLocalDate(zoneId: ZoneId = zone): LocalDate =
        Instant.ofEpochMilli(currentTimeMillis()).atZone(zoneId).toLocalDate()

    fun nowLocalDateTime(zoneId: ZoneId = zone): LocalDateTime =
        Instant.ofEpochMilli(currentTimeMillis()).atZone(zoneId).toLocalDateTime()

    fun nowZonedDateTime(zoneId: ZoneId = zone): ZonedDateTime =
        Instant.ofEpochMilli(currentTimeMillis()).atZone(zoneId)
}

/**
 * Production [Clock] implementation backed by the system clock and current system default time zone.
 */
class SystemClock @Inject constructor() : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override val zone: ZoneId
        get() = ZoneId.systemDefault()
    override fun nowLocalDate(zoneId: ZoneId): LocalDate = LocalDate.now(zoneId)
    override fun nowLocalDateTime(zoneId: ZoneId): LocalDateTime = LocalDateTime.now(zoneId)
    override fun nowZonedDateTime(zoneId: ZoneId): ZonedDateTime = ZonedDateTime.now(zoneId)
}

