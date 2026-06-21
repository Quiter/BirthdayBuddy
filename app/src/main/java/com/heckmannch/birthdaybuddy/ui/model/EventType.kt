package com.heckmannch.birthdaybuddy.ui.model

/**
 * Typsicherer Diskriminator für den aktuell angezeigten Ereignistyp.
 *
 * Ersetzt den zuvor verwendeten [String]-basierten Ansatz ("birthday", "anniversary", "name_day"),
 * um Tippfehler und stilles Fehlverhalten in der Filter- und Mapping-Logik zu verhindern.
 */
enum class EventType {
    /** Geburtstag – Standard-Ereignistyp */
    BIRTHDAY,

    /** Hochzeitstag – nur aktiv wenn "Weitere Ereignisse" aktiviert ist */
    ANNIVERSARY,

    /** Namenstag – nur aktiv wenn "Weitere Ereignisse" aktiviert ist */
    NAME_DAY,
}
