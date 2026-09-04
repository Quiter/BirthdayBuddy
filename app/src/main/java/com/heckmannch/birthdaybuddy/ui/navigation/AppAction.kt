package com.heckmannch.birthdaybuddy.ui.navigation

/**
 * Typsichere App-weite Navigations- und Steuerungs-Aktionen, die z. B. über Android-Intents
 * (App-Shortcuts, Widgets, Benachrichtigungen, AppFunctions) ausgelöst werden.
 *
 * Entkoppelt die UI- und ViewModel-Ebene vollständig von Android-Framework-APIs wie [android.content.Intent].
 */
sealed interface AppAction {

    /**
     * Navigiert direkt zum Einstellungs-Screen für Benachrichtigungen.
     */
    data object NavigateToNotifications : AppAction

    /**
     * Öffnet die Suchleiste auf dem Home-Screen und setzt den Fokus darauf.
     */
    data object OpenSearch : AppAction

    /**
     * Scrollt die Kontaktliste auf dem Home-Screen nach oben.
     */
    data object ScrollToTop : AppAction

    /**
     * Löst nach dem Anlegen eines Kontakts eine Synchronisation der Kontaktliste auf dem Home-Screen aus.
     */
    data object OpenAddContact : AppAction

    /**
     * Öffnet den Geburtstags-Picker-Dialog auf dem Home-Screen für einen bestimmten Kontakt
     * mit vorbefüllten Datumswerten (z. B. aufgerufen über Android AppFunctions).
     *
     * @property contactLookupKey Eindeutiger Lookup-Key des Kontakts.
     * @property year Geburtsjahr oder null, falls kein Jahr angegeben wurde.
     * @property month Geburtsmonat (1–12).
     * @property day Geburtstag im Monat (1–31).
     */
    data class OpenBirthdayPicker(
        val contactLookupKey: String,
        val year: Int?,
        val month: Int,
        val day: Int,
    ) : AppAction
}
