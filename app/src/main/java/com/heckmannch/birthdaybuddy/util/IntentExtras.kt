package com.heckmannch.birthdaybuddy.util

import android.content.Intent
import android.util.Log

/**
 * Zentrale Konstantenquelle und sichere Extraktions-Logik für alle Intent-Extra-Schlüssel der App.
 *
 * Durch die Nutzung dieser Konstanten und Hilfsfunktionen werden Tippfehler zur Compile-Zeit verhindert,
 * Type-Safety sichergestellt und Abstürze durch manipulierte oder inkompatible Intent-Payloads
 * (z. B. BadParcelableException, ClassCastException) abgefangen.
 */
object IntentExtras {
    private const val TAG = "IntentExtras"

    /** Weist MainActivity an, die Home-Liste nach oben zu scrollen. */
    const val SCROLL_TO_TOP = "SCROLL_TO_TOP"

    /** Weist MainActivity an, zum Benachrichtigungs-Einstellungsscreen zu navigieren. */
    const val NAVIGATE_TO_NOTIFICATIONS = "NAVIGATE_TO_NOTIFICATIONS"

    /** Weist MainActivity an, das Suchfeld zu öffnen und den Fokus darauf zu setzen. */
    const val OPEN_SEARCH = "OPEN_SEARCH"

    /** Weist MainActivity an, die Kontaktliste nach einem neu hinzugefügten Kontakt zu synchronisieren. */
    const val OPEN_ADD_CONTACT = "OPEN_ADD_CONTACT"

    /** AppFunctions Deep Link Extras */
    const val APPFN_CONTACT_ID = "APPFN_CONTACT_ID"
    const val APPFN_CONTACT_NAME = "APPFN_CONTACT_NAME"
    const val APPFN_BIRTHDAY_YEAR = "APPFN_BIRTHDAY_YEAR"
    const val APPFN_BIRTHDAY_MONTH = "APPFN_BIRTHDAY_MONTH"
    const val APPFN_BIRTHDAY_DAY = "APPFN_BIRTHDAY_DAY"

    /**
     * Liest ein Extra sicher aus einem [Intent] aus, fängt Exceptions ab und gibt bei Fehlern oder Fehlen den [defaultValue] zurück.
     */
    private inline fun <T> safeGet(
        intent: Intent?,
        key: String,
        defaultValue: T,
        extract: Intent.(String) -> T
    ): T {
        if (intent == null) return defaultValue
        return try {
            if (intent.hasExtra(key)) {
                intent.extract(key)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim sicheren Auslesen des Intent-Extras '$key'", e)
            defaultValue
        }
    }

    /**
     * Liest ein Extra sicher aus einem [Intent] aus und entfernt es anschließend zwingend aus dem Intent.
     * Fängt Exceptions ab, führt im Fehlerfall ein Fallback-removeExtra durch und gibt [defaultValue] zurück.
     */
    private inline fun <T> safeGetAndRemove(
        intent: Intent?,
        key: String,
        defaultValue: T,
        extract: Intent.(String) -> T
    ): T {
        if (intent == null) return defaultValue
        return try {
            if (intent.hasExtra(key)) {
                val value = intent.extract(key)
                intent.removeExtra(key)
                value
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim sicheren Auslesen/Entfernen des Intent-Extras '$key'", e)
            try {
                intent.removeExtra(key)
            } catch (cleanupException: Exception) {
                Log.w(TAG, "Bereinigung des Intent-Extras '$key' fehlgeschlagen", cleanupException)
            }
            defaultValue
        }
    }

    /**
     * Liest ein Boolean-Extra sicher aus einem [Intent] aus und entfernt es anschließend zwingend aus dem Intent.
     *
     * Fängt jegliche Type-Mismatch- (z.B. [ClassCastException]) oder Unparceling-Fehler (z.B. RuntimeExceptions)
     * ab, die durch externe, manipulierte oder inkompatible Intent-Payloads entstehen können.
     * Bereinigt den Intent zwingend mittels [Intent.removeExtra], damit das Extra bei Konfigurationsänderungen
     * (z.B. Bildschirm-Rotation) nicht versehentlich erneut verarbeitet wird.
     *
     * @param intent Der zu verarbeitende Intent.
     * @param key Der Schlüssel des auszulesenden Extras.
     * @param defaultValue Der Standardwert, falls das Extra nicht vorhanden ist oder ein Fehler auftritt.
     * @return Den ausgelesenen Boolean-Wert oder [defaultValue], falls nicht gefunden oder ungültig.
     */
    fun safeGetAndRemoveBooleanExtra(
        intent: Intent?,
        key: String,
        defaultValue: Boolean = false
    ): Boolean = safeGetAndRemove(intent, key, defaultValue) { k ->
        getBooleanExtra(k, defaultValue)
    }

    /**
     * Liest ein String-Extra sicher aus einem [Intent] aus und entfernt es anschließend zwingend aus dem Intent.
     *
     * Fängt jegliche Type-Mismatch- (z.B. [ClassCastException]) oder Unparceling-Fehler ab.
     * Bereinigt den Intent zwingend mittels [Intent.removeExtra].
     *
     * @param intent Der zu verarbeitende Intent.
     * @param key Der Schlüssel des auszulesenden Extras.
     * @param defaultValue Der Standardwert, falls das Extra nicht vorhanden ist oder ein Fehler auftritt.
     * @return Den ausgelesenen String-Wert oder [defaultValue], falls nicht gefunden oder ungültig.
     */
    fun safeGetAndRemoveStringExtra(
        intent: Intent?,
        key: String,
        defaultValue: String? = null
    ): String? = safeGetAndRemove(intent, key, defaultValue) { k ->
        getStringExtra(k) ?: defaultValue
    }

    /**
     * Liest ein Int-Extra sicher aus einem [Intent] aus und entfernt es anschließend zwingend aus dem Intent.
     *
     * Fängt jegliche Type-Mismatch- oder Unparceling-Fehler ab.
     * Bereinigt den Intent zwingend mittels [Intent.removeExtra].
     *
     * @param intent Der zu verarbeitende Intent.
     * @param key Der Schlüssel des auszulesenden Extras.
     * @param defaultValue Der Standardwert, falls das Extra nicht vorhanden ist oder ein Fehler auftritt.
     * @return Den ausgelesenen Int-Wert oder [defaultValue], falls nicht gefunden oder ungültig.
     */
    fun safeGetAndRemoveIntExtra(
        intent: Intent?,
        key: String,
        defaultValue: Int = -1
    ): Int = safeGetAndRemove(intent, key, defaultValue) { k ->
        getIntExtra(k, defaultValue)
    }

    /**
     * Liest ein Boolean-Extra sicher aus einem [Intent] aus, ohne den Intent zu mutieren.
     *
     * Fängt jegliche Type-Mismatch- (z.B. [ClassCastException]) oder Unparceling-Fehler ab.
     *
     * @param intent Der zu verarbeitende Intent.
     * @param key Der Schlüssel des auszulesenden Extras.
     * @param defaultValue Der Standardwert, falls das Extra nicht vorhanden ist oder ein Fehler auftritt.
     * @return Den ausgelesenen Boolean-Wert oder [defaultValue], falls nicht gefunden oder ungültig.
     */
    fun safeGetBooleanExtra(
        intent: Intent?,
        key: String,
        defaultValue: Boolean = false
    ): Boolean = safeGet(intent, key, defaultValue) { k ->
        getBooleanExtra(k, defaultValue)
    }

    /**
     * Liest ein String-Extra sicher aus einem [Intent] aus, ohne den Intent zu mutieren.
     *
     * Fängt jegliche Type-Mismatch- (z.B. [ClassCastException]) oder Unparceling-Fehler ab.
     *
     * @param intent Der zu verarbeitende Intent.
     * @param key Der Schlüssel des auszulesenden Extras.
     * @param defaultValue Der Standardwert, falls das Extra nicht vorhanden ist oder ein Fehler auftritt.
     * @return Den ausgelesenen String-Wert oder [defaultValue], falls nicht gefunden oder ungültig.
     */
    fun safeGetStringExtra(
        intent: Intent?,
        key: String,
        defaultValue: String? = null
    ): String? = safeGet(intent, key, defaultValue) { k ->
        getStringExtra(k) ?: defaultValue
    }

    /**
     * Liest ein Int-Extra sicher aus einem [Intent] aus.
     *
     * Fängt Type-Mismatch- und Unparceling-Fehler ab.
     */
    fun safeGetIntExtra(
        intent: Intent?,
        key: String,
        defaultValue: Int = -1
    ): Int = safeGet(intent, key, defaultValue) { k ->
        getIntExtra(k, defaultValue)
    }

    /**
     * Liest ein String-Array-Extra sicher aus einem [Intent] aus.
     *
     * Fängt Type-Mismatch- und Unparceling-Fehler ab.
     */
    fun safeGetStringArrayExtra(
        intent: Intent?,
        key: String
    ): Array<String> = safeGet(intent, key, emptyArray()) { k ->
        getStringArrayExtra(k) ?: emptyArray()
    }
}

/**
 * Extension-Funktion für [Intent?], um ein Boolean-Extra sicher auszulesen, ohne den Intent zu mutieren.
 *
 * @see IntentExtras.safeGetBooleanExtra
 */
fun Intent?.safeGetBooleanExtra(key: String, defaultValue: Boolean = false): Boolean =
    IntentExtras.safeGetBooleanExtra(this, key, defaultValue)

/**
 * Extension-Funktion für [Intent?], um ein String-Extra sicher auszulesen, ohne den Intent zu mutieren.
 *
 * @see IntentExtras.safeGetStringExtra
 */
fun Intent?.safeGetStringExtra(key: String, defaultValue: String? = null): String? =
    IntentExtras.safeGetStringExtra(this, key, defaultValue)

/**
 * Extension-Funktion für [Intent?], um ein Int-Extra sicher auszulesen, ohne den Intent zu mutieren.
 *
 * @see IntentExtras.safeGetIntExtra
 */
fun Intent?.safeGetIntExtra(key: String, defaultValue: Int = -1): Int =
    IntentExtras.safeGetIntExtra(this, key, defaultValue)

/**
 * Extension-Funktion für [Intent?], um ein Boolean-Extra sicher auszulesen und zwingend zu entfernen.
 *
 * @see IntentExtras.safeGetAndRemoveBooleanExtra
 */
fun Intent?.safeGetAndRemoveBooleanExtra(key: String, defaultValue: Boolean = false): Boolean =
    IntentExtras.safeGetAndRemoveBooleanExtra(this, key, defaultValue)

/**
 * Extension-Funktion für [Intent?], um ein String-Extra sicher auszulesen und zwingend zu entfernen.
 *
 * @see IntentExtras.safeGetAndRemoveStringExtra
 */
fun Intent?.safeGetAndRemoveStringExtra(key: String, defaultValue: String? = null): String? =
    IntentExtras.safeGetAndRemoveStringExtra(this, key, defaultValue)

/**
 * Extension-Funktion für [Intent?], um ein Int-Extra sicher auszulesen und zwingend zu entfernen.
 *
 * @see IntentExtras.safeGetAndRemoveIntExtra
 */
fun Intent?.safeGetAndRemoveIntExtra(key: String, defaultValue: Int = -1): Int =
    IntentExtras.safeGetAndRemoveIntExtra(this, key, defaultValue)
