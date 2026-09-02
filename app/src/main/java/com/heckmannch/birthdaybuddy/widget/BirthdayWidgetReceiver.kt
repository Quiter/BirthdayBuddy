package com.heckmannch.birthdaybuddy.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver and entry point for the Android AppWidget system.
 *
 * Connects the system widget lifecycle to Glance by binding [BirthdayWidget]
 * via [GlanceAppWidgetReceiver].
 */
class BirthdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthdayWidget()
}
