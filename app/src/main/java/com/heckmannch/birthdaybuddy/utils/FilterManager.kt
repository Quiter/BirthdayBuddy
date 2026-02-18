package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Erweiterungseigenschaft für Context, um eine Instanz von DataStore zu erstellen.
 * DataStore ist der moderne Ersatz für SharedPreferences und speichert Daten asynchron und sicher.
 */
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Der FilterManager kümmert sich um das persistente Speichern und Laden aller Benutzereinstellungen.
 * Er nutzt Jetpack DataStore, um Daten als reaktive "Flows" bereitzustellen.
 */
class FilterManager(val context: Context) {

    // --- KEYS FÜR DIE DATENSPEICHERUNG ---

    // Whitelist-Ansatz: Wir speichern, welche Labels der Nutzer AKTIV für die Anzeige ausgewählt hat.
    private val SELECTED_LABELS_KEY = stringSetPreferencesKey("selected_labels")
    private val WIDGET_SELECTED_LABELS_KEY = stringSetPreferencesKey("widget_selected_labels")

    // Blacklist-Ansatz: Labels, die global oder im Widget komplett ignoriert werden sollen.
    private val EXCLUDED_LABELS_KEY = stringSetPreferencesKey("excluded_labels")
    private val WIDGET_EXCLUDED_LABELS_KEY = stringSetPreferencesKey("widget_excluded_labels")
    
    // Einstellung für Labels, die im Drawer (Seitenmenü) nicht auftauchen sollen.
    private val HIDDEN_DRAWER_LABELS_KEY = stringSetPreferencesKey("hidden_drawer_labels")

    // Einstellungen für die Benachrichtigungen (Zeitpunkt und Vorlauf).
    private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
    private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
    private val NOTIFICATION_DAYS_KEY = stringSetPreferencesKey("notification_days")

    // Anzahl der maximal anzuzeigenden Personen im Widget.
    private val WIDGET_ITEM_COUNT_KEY = intPreferencesKey("widget_item_count")


    // --- FLOWS (ZUM LESEN DER DATEN) ---
    // Diese Flows emittieren jedes Mal einen neuen Wert, wenn sich die Daten im DataStore ändern.

    /** Flow für die in der Hauptliste aktiven Labels. */
    val selectedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[SELECTED_LABELS_KEY] ?: emptySet() }
    
    /** Flow für global blockierte Labels. */
    val excludedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[EXCLUDED_LABELS_KEY] ?: emptySet() }
    
    /** Flow für Labels, die im Seitenmenü versteckt sind. */
    val hiddenDrawerLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[HIDDEN_DRAWER_LABELS_KEY] ?: emptySet() }
    
    /** Die Stunde, zu der Benachrichtigungen gesendet werden (Standard: 09:00). */
    val notificationHourFlow: Flow<Int> = context.dataStore.data.map { it[NOTIFICATION_HOUR_KEY] ?: 9 }
    
    /** Die Minute, zu der Benachrichtigungen gesendet werden. */
    val notificationMinuteFlow: Flow<Int> = context.dataStore.data.map { it[NOTIFICATION_MINUTE_KEY] ?: 0 }
    
    /** Die Tage vor dem Geburtstag, an denen eine Info kommen soll (Standard: Am Tag selbst und 7 Tage vorher). */
    val notificationDaysFlow: Flow<Set<String>> = context.dataStore.data.map { it[NOTIFICATION_DAYS_KEY] ?: setOf("0", "7") }
    
    /** Flow für die im Widget erlaubten Labels. */
    val widgetSelectedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[WIDGET_SELECTED_LABELS_KEY] ?: emptySet() }
    
    /** Flow für die im Widget blockierten Labels. */
    val widgetExcludedLabelsFlow: Flow<Set<String>> = context.dataStore.data.map { it[WIDGET_EXCLUDED_LABELS_KEY] ?: emptySet() }
    
    /** Anzahl der Personen im Widget (Standard: 1). */
    val widgetItemCountFlow: Flow<Int> = context.dataStore.data.map { it[WIDGET_ITEM_COUNT_KEY] ?: 1 }


    // --- SPEICHERFUNKTIONEN (SUSPEND FUNS) ---
    // Diese Funktionen müssen in einer Coroutine aufgerufen werden, da sie Schreiboperationen durchführen.

    suspend fun saveSelectedLabels(labels: Set<String>) { context.dataStore.edit { it[SELECTED_LABELS_KEY] = labels } }
    suspend fun saveExcludedLabels(labels: Set<String>) { context.dataStore.edit { it[EXCLUDED_LABELS_KEY] = labels } }
    suspend fun saveHiddenDrawerLabels(labels: Set<String>) { context.dataStore.edit { it[HIDDEN_DRAWER_LABELS_KEY] = labels } }
    
    /** Speichert die Uhrzeit für die tägliche Prüfung der Geburtstage. */
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
}
