package com.heckmannch.birthdaybuddy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class FilterManager(val context: Context) {

    private val SELECTED_LABELS_KEY = stringSetPreferencesKey("selected_labels")
    private val WIDGET_SELECTED_LABELS_KEY = stringSetPreferencesKey("widget_selected_labels")
    private val EXCLUDED_LABELS_KEY = stringSetPreferencesKey("excluded_labels")
    private val WIDGET_EXCLUDED_LABELS_KEY = stringSetPreferencesKey("widget_excluded_labels")
    private val HIDDEN_DRAWER_LABELS_KEY = stringSetPreferencesKey("hidden_drawer_labels")
    private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
    private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
    private val NOTIFICATION_DAYS_KEY = stringSetPreferencesKey("notification_days")
    private val WIDGET_ITEM_COUNT_KEY = intPreferencesKey("widget_item_count")
    
    // Flag für die erste Initialisierung
    private val IS_INITIALIZED_KEY = booleanPreferencesKey("is_initialized")

    val selectedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[SELECTED_LABELS_KEY] ?: emptySet() }
    val excludedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[EXCLUDED_LABELS_KEY] ?: emptySet() }
    val hiddenDrawerLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[HIDDEN_DRAWER_LABELS_KEY] ?: emptySet() }
    val notificationHourFlow: Flow<Int> = context.dataStore.data.map { it[NOTIFICATION_HOUR_KEY] ?: 9 }
    val notificationMinuteFlow: Flow<Int> = context.dataStore.data.map { it[NOTIFICATION_MINUTE_KEY] ?: 0 }
    val notificationDaysFlow: Flow<Set<String>> = context.dataStore.data.map { it[NOTIFICATION_DAYS_KEY] ?: setOf("0", "7") }
    val widgetSelectedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[WIDGET_SELECTED_LABELS_KEY] ?: emptySet() }
    val widgetExcludedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[WIDGET_EXCLUDED_LABELS_KEY] ?: emptySet() }
    val widgetItemCountFlow: Flow<Int> = context.dataStore.data.map { it[WIDGET_ITEM_COUNT_KEY] ?: 3 }
    
    val isInitializedFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_INITIALIZED_KEY] ?: false }

    suspend fun saveSelectedLabels(labels: Set<String>) { context.dataStore.edit { it[SELECTED_LABELS_KEY] = labels } }
    suspend fun saveExcludedLabels(labels: Set<String>) { context.dataStore.edit { it[EXCLUDED_LABELS_KEY] = labels } }
    suspend fun saveHiddenDrawerLabels(labels: Set<String>) { context.dataStore.edit { it[HIDDEN_DRAWER_LABELS_KEY] = labels } }
    
    suspend fun saveNotificationTime(hour: Int, minute: Int) { 
        context.dataStore.edit { 
            it[NOTIFICATION_HOUR_KEY] = hour
            it[NOTIFICATION_MINUTE_KEY] = minute 
        } 
    }

    suspend fun saveNotificationDays(days: Set<String>) { context.dataStore.edit { it[NOTIFICATION_DAYS_KEY] = days } }
    suspend fun saveWidgetSelectedLabels(labels: Set<String>) { context.dataStore.edit { it[WIDGET_SELECTED_LABELS_KEY] = labels } }
    suspend fun saveWidgetExcludedLabels(labels: Set<String>) { context.dataStore.edit { it[WIDGET_EXCLUDED_LABELS_KEY] = labels } }
    suspend fun saveWidgetItemCount(count: Int) { context.dataStore.edit { it[WIDGET_ITEM_COUNT_KEY] = count } }
    
    suspend fun setInitialized(value: Boolean) { context.dataStore.edit { it[IS_INITIALIZED_KEY] = value } }
}
