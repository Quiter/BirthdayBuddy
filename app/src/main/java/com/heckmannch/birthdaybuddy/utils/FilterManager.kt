package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class FilterManager(private val context: Context) {
    private val SELECTED_LABELS_KEY = stringSetPreferencesKey("selected_labels")
    private val EXCLUDED_LABELS_KEY = stringSetPreferencesKey("excluded_labels")
    private val HIDDEN_DRAWER_LABELS_KEY = stringSetPreferencesKey("hidden_drawer_labels")

    // NEU: Speicherschlüssel für Benachrichtigungen
    private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
    private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
    private val NOTIFICATION_DAYS_KEY = stringSetPreferencesKey("notification_days")

    val selectedLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_LABELS_KEY] ?: emptySet() }

    val excludedLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[EXCLUDED_LABELS_KEY] ?: emptySet() }

    val hiddenDrawerLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[HIDDEN_DRAWER_LABELS_KEY] ?: emptySet() }

    // NEU: Flows für Benachrichtigungen (Standard: 09:00 Uhr, 0 und 7 Tage vorher)
    val notificationHourFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_HOUR_KEY] ?: 9 }

    val notificationMinuteFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_MINUTE_KEY] ?: 0 }

    val notificationDaysFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_DAYS_KEY] ?: setOf("0", "7") }

    suspend fun saveSelectedLabels(labels: Set<String>) {
        context.dataStore.edit { it[SELECTED_LABELS_KEY] = labels }
    }

    suspend fun saveExcludedLabels(labels: Set<String>) {
        context.dataStore.edit { it[EXCLUDED_LABELS_KEY] = labels }
    }

    suspend fun saveHiddenDrawerLabels(labels: Set<String>) {
        context.dataStore.edit { it[HIDDEN_DRAWER_LABELS_KEY] = labels }
    }

    // NEU: Speichern der Uhrzeit
    suspend fun saveNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[NOTIFICATION_HOUR_KEY] = hour
            it[NOTIFICATION_MINUTE_KEY] = minute
        }
    }

    // NEU: Speichern der Vorlauf-Tage
    suspend fun saveNotificationDays(days: Set<String>) {
        context.dataStore.edit { it[NOTIFICATION_DAYS_KEY] = days }
    }
}