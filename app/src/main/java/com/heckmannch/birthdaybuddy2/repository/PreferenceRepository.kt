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
        private val SWIPE_HINT_SHOWN = booleanPreferencesKey("swipe_hint_shown")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: false
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

    suspend fun setSwipeHintShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SWIPE_HINT_SHOWN] = shown
        }
    }
}
