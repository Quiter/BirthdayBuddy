package com.heckmannch.birthdaybuddy.util

/**
 * Zentrale Konstantenquelle für alle Intent-Extra-Schlüssel der App.
 *
 * Durch die Nutzung dieser Konstanten anstelle von String-Literalen werden
 * Tippfehler zur Compile-Zeit erkannt und verhindert stummes Fehlverhalten
 * zwischen Producern (Widget, NotificationHelper) und Consumern (MainActivity).
 */
object IntentExtras {
    /** Weist MainActivity an, die Home-Liste nach oben zu scrollen. */
    const val SCROLL_TO_TOP = "SCROLL_TO_TOP"

    /** Weist MainActivity an, zum Benachrichtigungs-Einstellungsscreen zu navigieren. */
    const val NAVIGATE_TO_NOTIFICATIONS = "NAVIGATE_TO_NOTIFICATIONS"

    /** Weist MainActivity an, das Suchfeld zu öffnen und den Fokus darauf zu setzen. */
    const val OPEN_SEARCH = "OPEN_SEARCH"

    /** Weist MainActivity an, die Kontaktliste nach einem neu hinzugefügten Kontakt zu synchronisieren. */
    const val OPEN_ADD_CONTACT = "OPEN_ADD_CONTACT"
}
