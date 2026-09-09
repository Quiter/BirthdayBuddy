package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [WidgetUpdater] that coordinates Glance widget updates and exact midnight alarms.
 *
 * Widget-Update-Strategie (Single-Path):
 * - [updateWidget]: Führt eine direkte, unmittelbare Aktualisierung aller [BirthdayWidget]-Instanzen
 *   durch ([androidx.glance.appwidget.updateAll]).
 * - [scheduleDailyUpdate]: Beauftragt [AlarmScheduler.scheduleNextWidgetUpdateAlarm] mit dem Setzen
 *   eines Doze-resistenten Alarms ([android.app.AlarmManager.setExactAndAllowWhileIdle]) für 00:01 Uhr.
 * - WorkManager wird nicht zur Vorab-Planung verwendet, sondern kommt erst bei Auslösen des Alarms
 *   als robuster Execution- und Retry-Runner zum Einsatz.
 */
class BirthdayWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmScheduler: AlarmScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WidgetUpdater {
    override suspend fun updateWidget(): Unit = withContext(ioDispatcher) {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("BirthdayWidgetUpdater", "Widget update failed", e)
        }
    }

    override fun scheduleDailyUpdate() {
        try {
            alarmScheduler.scheduleNextWidgetUpdateAlarm()
        } catch (e: Exception) {
            Log.e("BirthdayWidgetUpdater", "Widget scheduling failed", e)
        }
    }
}
