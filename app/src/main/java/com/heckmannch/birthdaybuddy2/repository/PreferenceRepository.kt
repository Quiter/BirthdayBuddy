package com.heckmannch.birthdaybuddy2.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    companion object {
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        private val SWIPE_HINT_SHOWN = booleanPreferencesKey("swipe_hint_shown")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: false
        }

    val notificationHour: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_HOUR] ?: 9
        }

    val notificationMinute: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_MINUTE] ?: 0
        }

    val swipeHintShown: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SWIPE_HINT_SHOWN] ?: false
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR] = hour
            preferences[NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setSwipeHintShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_HINT_SHOWN] = shown
        }
    }
}
