package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Wir erstellen den DataStore unter dem Namen "settings"
private val Context.dataStore by preferencesDataStore(name = "settings")

class FilterManager(private val context: Context) {

    // Der Schlüssel für unsere Label-Liste
    private val SELECTED_LABELS_KEY = stringSetPreferencesKey("selected_labels")

    // Lädt die gespeicherten Labels (als Datenstrom/Flow)
    val selectedLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_LABELS_KEY] ?: emptySet()
        }

    // Speichert die Labels dauerhaft
    suspend fun saveSelectedLabels(labels: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LABELS_KEY] = labels
        }
    }
}