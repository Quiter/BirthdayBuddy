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
    val lastBackgroundTime: Long,
    val isInitialized: Boolean,
    val showLabelManagerIntro: Boolean,
    val theme: Int // 0: System, 1: Light, 2: Dark
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
        val LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
        val IS_INITIALIZED = booleanPreferencesKey("is_initialized")
        val SHOW_LABEL_MANAGER_INTRO = booleanPreferencesKey("show_label_manager_intro")
        val THEME = intPreferencesKey("theme")
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
            lastBackgroundTime = prefs[Keys.LAST_BACKGROUND_TIME] ?: 0L,
            isInitialized = prefs[Keys.IS_INITIALIZED] ?: false,
            showLabelManagerIntro = prefs[Keys.SHOW_LABEL_MANAGER_INTRO] ?: true,
            theme = prefs[Keys.THEME] ?: 0
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
    val lastBackgroundTimeFlow = preferencesFlow.map { it.lastBackgroundTime }
    val isInitializedFlow = preferencesFlow.map { it.isInitialized }
    val showLabelManagerIntroFlow = preferencesFlow.map { it.showLabelManagerIntro }
    val themeFlow = preferencesFlow.map { it.theme }

    suspend fun saveSelectedLabels(labels: Set<String>) = edit { it[Keys.SELECTED_LABELS] = labels }
    suspend fun saveExcludedLabels(labels: Set<String>) = edit { it[Keys.EXCLUDED_LABELS] = labels }
    suspend fun saveHiddenDrawerLabels(labels: Set<String>) = edit { it[Keys.HIDDEN_DRAWER_LABELS] = labels }
    suspend fun saveWidgetSelectedLabels(labels: Set<String>) = edit { it[Keys.WIDGET_SELECTED_LABELS] = labels }
    suspend fun saveWidgetExcludedLabels(labels: Set<String>) = edit { it[Keys.WIDGET_EXCLUDED_LABELS] = labels }
    suspend fun saveNotificationSelectedLabels(labels: Set<String>) = edit { it[Keys.NOTIFICATION_SELECTED_LABELS] = labels }
    suspend fun saveNotificationExcludedLabels(labels: Set<String>) = edit { it[Keys.NOTIFICATION_EXCLUDED_LABELS] = labels }
    suspend fun saveLastBackgroundTime(time: Long) = edit { it[Keys.LAST_BACKGROUND_TIME] = time }
    suspend fun setInitialized(value: Boolean) = edit { it[Keys.IS_INITIALIZED] = value }
    suspend fun setShowLabelManagerIntro(value: Boolean) = edit { it[Keys.SHOW_LABEL_MANAGER_INTRO] = value }
    suspend fun saveTheme(theme: Int) = edit { it[Keys.THEME] = theme }
    
    suspend fun saveNotificationTime(hour: Int, minute: Int) = context.dataStore.edit { 
        it[Keys.NOTIFICATION_HOUR] = hour
        it[Keys.NOTIFICATION_MINUTE] = minute 
    }
    
    suspend fun saveNotificationDays(days: Set<String>) = edit { it[Keys.NOTIFICATION_DAYS] = days }

    private suspend fun edit(action: (MutablePreferences) -> Unit) {
        context.dataStore.edit { action(it) }
    }
}
