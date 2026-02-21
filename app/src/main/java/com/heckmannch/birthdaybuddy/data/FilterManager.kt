package com.heckmannch.birthdaybuddy.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

data class UserPreferences(
    val selectedLabels: Set<String>,
    val excludedLabels: Set<String>,
    val hiddenDrawerLabels: Set<String>,
    val widgetSelectedLabels: Set<String>,
    val widgetExcludedLabels: Set<String>,
    val notificationSelectedLabels: Set<String>,
    val notificationExcludedLabels: Set<String>,
    val notificationHour: Int,
    val notificationMinute: Int,
    val notificationDays: Set<String>,
    val widgetItemCount: Int,
    val isInitialized: Boolean
)

@Singleton
class FilterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val SELECTED_LABELS = stringSetPreferencesKey("selected_labels")
        val EXCLUDED_LABELS = stringSetPreferencesKey("excluded_labels")
        val HIDDEN_DRAWER_LABELS = stringSetPreferencesKey("hidden_drawer_labels")
        val WIDGET_SELECTED_LABELS = stringSetPreferencesKey("widget_selected_labels")
        val WIDGET_EXCLUDED_LABELS = stringSetPreferencesKey("widget_excluded_labels")
        val NOTIFICATION_SELECTED_LABELS = stringSetPreferencesKey("notification_selected_labels")
        val NOTIFICATION_EXCLUDED_LABELS = stringSetPreferencesKey("notification_excluded_labels")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val NOTIFICATION_DAYS = stringSetPreferencesKey("notification_days")
        val WIDGET_ITEM_COUNT = intPreferencesKey("widget_item_count")
        val IS_INITIALIZED = booleanPreferencesKey("is_initialized")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            selectedLabels = prefs[Keys.SELECTED_LABELS] ?: emptySet(),
            excludedLabels = prefs[Keys.EXCLUDED_LABELS] ?: emptySet(),
            hiddenDrawerLabels = prefs[Keys.HIDDEN_DRAWER_LABELS] ?: emptySet(),
            widgetSelectedLabels = prefs[Keys.WIDGET_SELECTED_LABELS] ?: emptySet(),
            widgetExcludedLabels = prefs[Keys.WIDGET_EXCLUDED_LABELS] ?: emptySet(),
            notificationSelectedLabels = prefs[Keys.NOTIFICATION_SELECTED_LABELS] ?: emptySet(),
            notificationExcludedLabels = prefs[Keys.NOTIFICATION_EXCLUDED_LABELS] ?: emptySet(),
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 9,
            notificationMinute = prefs[Keys.NOTIFICATION_MINUTE] ?: 0,
            notificationDays = prefs[Keys.NOTIFICATION_DAYS] ?: setOf("0", "7"),
            widgetItemCount = prefs[Keys.WIDGET_ITEM_COUNT] ?: 3,
            isInitialized = prefs[Keys.IS_INITIALIZED] ?: false
        )
    }

    val selectedLabelsFlow = preferencesFlow.map { it.selectedLabels }
    val excludedLabelsFlow = preferencesFlow.map { it.excludedLabels }
    val hiddenDrawerLabelsFlow = preferencesFlow.map { it.hiddenDrawerLabels }
    val widgetSelectedLabelsFlow = preferencesFlow.map { it.widgetSelectedLabels }
    val widgetExcludedLabelsFlow = preferencesFlow.map { it.widgetExcludedLabels }
    val notificationSelectedLabelsFlow = preferencesFlow.map { it.notificationSelectedLabels }
    val notificationExcludedLabelsFlow = preferencesFlow.map { it.notificationExcludedLabels }
    val notificationHourFlow = preferencesFlow.map { it.notificationHour }
    val notificationMinuteFlow = preferencesFlow.map { it.notificationMinute }
    val notificationDaysFlow = preferencesFlow.map { it.notificationDays }
    val widgetItemCountFlow = preferencesFlow.map { it.widgetItemCount }
    val isInitializedFlow = preferencesFlow.map { it.isInitialized }

    suspend fun saveSelectedLabels(labels: Set<String>) = edit { it[Keys.SELECTED_LABELS] = labels }
    suspend fun saveExcludedLabels(labels: Set<String>) = edit { it[Keys.EXCLUDED_LABELS] = labels }
    suspend fun saveHiddenDrawerLabels(labels: Set<String>) = edit { it[Keys.HIDDEN_DRAWER_LABELS] = labels }
    suspend fun saveWidgetSelectedLabels(labels: Set<String>) = edit { it[Keys.WIDGET_SELECTED_LABELS] = labels }
    suspend fun saveWidgetExcludedLabels(labels: Set<String>) = edit { it[Keys.WIDGET_EXCLUDED_LABELS] = labels }
    suspend fun saveNotificationSelectedLabels(labels: Set<String>) = edit { it[Keys.NOTIFICATION_SELECTED_LABELS] = labels }
    suspend fun saveNotificationExcludedLabels(labels: Set<String>) = edit { it[Keys.NOTIFICATION_EXCLUDED_LABELS] = labels }
    suspend fun saveWidgetItemCount(count: Int) = edit { it[Keys.WIDGET_ITEM_COUNT] = count }
    suspend fun setInitialized(value: Boolean) = edit { it[Keys.IS_INITIALIZED] = value }
    
    suspend fun saveNotificationTime(hour: Int, minute: Int) = context.dataStore.edit { 
        it[Keys.NOTIFICATION_HOUR] = hour
        it[Keys.NOTIFICATION_MINUTE] = minute 
    }
    
    suspend fun saveNotificationDays(days: Set<String>) = edit { it[Keys.NOTIFICATION_DAYS] = days }

    private suspend fun edit(action: (MutablePreferences) -> Unit) {
        context.dataStore.edit { action(it) }
    }
}
