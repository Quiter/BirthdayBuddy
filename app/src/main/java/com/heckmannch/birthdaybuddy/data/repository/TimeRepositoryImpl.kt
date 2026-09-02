package com.heckmannch.birthdaybuddy.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementierung von [TimeRepository], die das aktuelle Datum bereitstellt und automatisch aktualisiert.
 *
 * Registriert einen [BroadcastReceiver] für System-Ereignisse (Datumswechsel, Zeitzonenwechsel,
 * manuelle Zeitänderungen) und führt parallel eine Coroutine-Schleife bis Mitternacht aus.
 *
 * @property context Der [ApplicationContext] zur Registrierung des BroadcastReceivers.
 */
class TimeRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TimeRepository {

    /**
     * Ein Flow, der das aktuelle Datum emittiert und sich automatisch aktualisiert:
     * - Sofortige Emission beim Abonnieren ([LocalDate.now]).
     * - Reaktiv auf System-Broadcasts ([Intent.ACTION_DATE_CHANGED], [Intent.ACTION_TIMEZONE_CHANGED], [Intent.ACTION_TIME_CHANGED]).
     * - Zeitgesteuert um Mitternacht (+1s Puffer) über eine parallele Coroutine-Schleife.
     * - Unnötige Re-Emissions werden via [distinctUntilChanged] gefiltert.
     */
    override val currentDate: Flow<LocalDate> = callbackFlow {
        // Sofortige Emission des aktuellen Datums
        trySend(LocalDate.now())

        // BroadcastReceiver für System-Datums- und Zeitanpassungen
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(LocalDate.now())
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Parallele Coroutine-Schleife, die gezielt zu Mitternacht aufwacht
        val midnightJob = launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisUntilMidnight = ChronoUnit.MILLIS.between(now, nextMidnight)

                // Warte bis Mitternacht + 1 Sekunde Puffer
                delay((millisUntilMidnight + 1000).milliseconds)
                trySend(LocalDate.now())
            }
        }

        awaitClose {
            midnightJob.cancel()
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()
}

