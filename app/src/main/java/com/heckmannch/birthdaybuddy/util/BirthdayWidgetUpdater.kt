package com.heckmannch.birthdaybuddy.util

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BirthdayWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {
    override suspend fun updateWidget() {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("BirthdayWidgetUpdater", "Widget update failed", e)
        }
    }
}
