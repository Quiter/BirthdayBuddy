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
    ): Boolean {
        if (intent == null) return defaultValue
        return try {
            if (intent.hasExtra(key)) {
                val value = intent.getBooleanExtra(key, defaultValue)
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
     * Liest ein Int-Extra sicher aus einem [Intent] aus.
     *
     * Fängt Type-Mismatch- und Unparceling-Fehler ab.
     */
    fun safeGetIntExtra(
        intent: Intent?,
        key: String,
        defaultValue: Int = -1
    ): Int {
        if (intent == null) return defaultValue
        return try {
            if (intent.hasExtra(key)) {
                intent.getIntExtra(key, defaultValue)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim sicheren Auslesen des Int-Extras '$key'", e)
            defaultValue
        }
    }

    /**
     * Liest ein String-Array-Extra sicher aus einem [Intent] aus.
     *
     * Fängt Type-Mismatch- und Unparceling-Fehler ab.
     */
    fun safeGetStringArrayExtra(
        intent: Intent?,
        key: String
    ): Array<String> {
        if (intent == null) return emptyArray()
        return try {
            if (intent.hasExtra(key)) {
                intent.getStringArrayExtra(key) ?: emptyArray()
            } else {
                emptyArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler beim sicheren Auslesen des StringArray-Extras '$key'", e)
            emptyArray()
        }
    }
}

/**
 * Extension-Funktion für [Intent?], um ein Boolean-Extra sicher auszulesen und zwingend zu entfernen.
 *
 * @see IntentExtras.safeGetAndRemoveBooleanExtra
 */
fun Intent?.safeGetAndRemoveBooleanExtra(key: String, defaultValue: Boolean = false): Boolean =
    IntentExtras.safeGetAndRemoveBooleanExtra(this, key, defaultValue)

