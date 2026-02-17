package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class FilterManager(private val context: Context) {
    private val SELECTED_LABELS_KEY = stringSetPreferencesKey("selected_labels")
    private val EXCLUDED_LABELS_KEY = stringSetPreferencesKey("excluded_labels")
    // NEU: Ein dritter Schlüssel für die versteckten Menü-Einträge
    private val HIDDEN_DRAWER_LABELS_KEY = stringSetPreferencesKey("hidden_drawer_labels")

    val selectedLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_LABELS_KEY] ?: emptySet() }

    val excludedLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[EXCLUDED_LABELS_KEY] ?: emptySet() }

    // NEU: Flow zum Laden der versteckten Labels
    val hiddenDrawerLabelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[HIDDEN_DRAWER_LABELS_KEY] ?: emptySet() }

    suspend fun saveSelectedLabels(labels: Set<String>) {
        context.dataStore.edit { it[SELECTED_LABELS_KEY] = labels }
    }

    suspend fun saveExcludedLabels(labels: Set<String>) {
        context.dataStore.edit { it[EXCLUDED_LABELS_KEY] = labels }
    }

    // NEU: Funktion zum Speichern
    suspend fun saveHiddenDrawerLabels(labels: Set<String>) {
        context.dataStore.edit { it[HIDDEN_DRAWER_LABELS_KEY] = labels }
    }
}