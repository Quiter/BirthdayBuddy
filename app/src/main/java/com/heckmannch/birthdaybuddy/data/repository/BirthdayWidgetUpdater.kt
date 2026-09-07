package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class BirthdayWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmScheduler: AlarmScheduler,
) : WidgetUpdater {
    override suspend fun updateWidget() {
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
