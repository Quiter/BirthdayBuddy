package com.heckmannch.birthdaybuddy.util

import android.content.Intent
import com.heckmannch.birthdaybuddy.ui.navigation.AppAction

/**
 * Parser zur Umwandlung von eingehenden Android [Intent]-Payloads in typsichere [AppAction]-Instanzen.
 *
 * Kapselt das sichere Auslesen von Intent-Extras (z. B. aus App-Shortcuts, Widgets, Benachrichtigungen
 * oder AppFunctions) und stellt sicher, dass UI-Komponenten und ViewModels frei von direkten
 * Android-Framework-Abhängigkeiten bleiben.
 */
object IntentParser {

    /**
     * Parst einen übergebenen Android [Intent] und gibt die entsprechende [AppAction] zurück,
     * oder `null`, falls der Intent keine bekannte Aktion enthält oder null ist.
     *
     * @param intent Der zu parsende Android-Intent.
     * @return Die ermittelte [AppAction] oder `null`.
     */
    fun parse(intent: Intent?): AppAction? {
        if (intent == null) return null

        if (intent.safeGetBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)) {
            return AppAction.NavigateToNotifications
        }

        val appFnContactId = intent.safeGetStringExtra(IntentExtras.APPFN_CONTACT_ID)
        if (!appFnContactId.isNullOrBlank()) {
            val yearExtra = intent.safeGetIntExtra(IntentExtras.APPFN_BIRTHDAY_YEAR, NO_YEAR_MARKER)
            val month = intent.safeGetIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1)
            val day = intent.safeGetIntExtra(IntentExtras.APPFN_BIRTHDAY_DAY, -1)
            val year = if (yearExtra > 0 && yearExtra != NO_YEAR_MARKER) yearExtra else null

            return AppAction.OpenBirthdayPicker(
                contactLookupKey = appFnContactId,
                year = year,
                month = month,
                day = day,
            )
        }

        if (intent.safeGetBooleanExtra(IntentExtras.OPEN_SEARCH)) {
            return AppAction.OpenSearch
        }

        if (intent.safeGetBooleanExtra(IntentExtras.SCROLL_TO_TOP)) {
            return AppAction.ScrollToTop
        }

        if (intent.safeGetBooleanExtra(IntentExtras.OPEN_ADD_CONTACT)) {
            return AppAction.OpenAddContact
        }

        return null
    }
}
