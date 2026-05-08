package com.heckmannch.birthdaybuddy2.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeRepository @Inject constructor() {
    /**
     * Ein Flow, der das aktuelle Datum emittiert und sich automatisch um Mitternacht aktualisiert.
     */
    val currentDate: Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())
            
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val millisUntilMidnight = ChronoUnit.MILLIS.between(now, nextMidnight)
            
            // Warte bis Mitternacht + 1 Sekunde Puffer
            delay(millisUntilMidnight + 1000)
        }
    }.distinctUntilChanged()
}
