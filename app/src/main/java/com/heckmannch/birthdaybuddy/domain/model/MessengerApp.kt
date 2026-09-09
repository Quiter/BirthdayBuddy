package com.heckmannch.birthdaybuddy.domain.model

/**
 * Unterstützte Messenger-Apps für Direktaktionen aus der Geburtstagskarte.
 *
 * Reines Kotlin-Domain-Enum ohne Android- oder UI-Framework-Abhängigkeiten.
 * Beinhaltet den Paketnamen der jeweiligen App zur Identifikation und Verlinkung.
 */
enum class MessengerApp(
    val packageName: String,
) {
    WHATSAPP(
        packageName = "com.whatsapp",
    ),
    SIGNAL(
        packageName = "org.thoughtcrime.securesms",
    ),
    THREEMA(
        packageName = "ch.threema.app",
    ),
    MESSENGER(
        packageName = "com.facebook.orca",
    ),
    TELEGRAM(
        packageName = "org.telegram.messenger",
    ),
    VIBER(
        packageName = "com.viber.voip",
    ),
    GOOGLE_MEET(
        packageName = "com.google.android.apps.tachyon",
    ),
    SKYPE(
        packageName = "com.skype.raider",
    ),
    DISCORD(
        packageName = "com.discord",
    );

    companion object
}
