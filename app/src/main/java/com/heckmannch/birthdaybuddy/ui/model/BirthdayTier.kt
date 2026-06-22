package com.heckmannch.birthdaybuddy.ui.model

/**
 * Klassifiziert das visuelle Tier eines Kontakts für seinen Geburtstag.
 *
 * Die Berechnung erfolgt einmalig im [com.heckmannch.birthdaybuddy.data.mapper.ContactMapper]
 * und wird über [com.heckmannch.birthdaybuddy.ui.model.ContactUiModel.birthdayTier] an die UI
 * weitergegeben. Dadurch wird die doppelte Inline-Logik in BirthdayItem.kt vermieden.
 *
 * Prioritätsreihenfolge (wichtig, da sich Bedingungen überschneiden können):
 * 1. [MILESTONE_GOLD]   – Rundes Jubiläum (teilbar durch 10)
 * 2. [MILESTONE_SILVER] – Halb-Jubiläum (teilbar durch 5, aber nicht durch 10)
 * 3. [CHILD]            – Kindesalter (0–9 Jahre)
 * 4. [REGULAR]          – Alle anderen Fälle, inkl. Geburtstage ohne Jahreszahl
 */
enum class BirthdayTier {
    /** Alter ist durch 10 teilbar (z. B. 10, 20, 30, …). Goldene Hervorhebung. */
    MILESTONE_GOLD,

    /** Alter ist durch 5 teilbar, aber nicht durch 10 (z. B. 5, 15, 25, …). Silberne Hervorhebung. */
    MILESTONE_SILVER,

    /** Alter liegt zwischen 0 und 9 Jahren (inklusive). Bunte Kinder-Hervorhebung. */
    CHILD,

    /** Alle anderen Fälle, inkl. Geburtstage ohne gespeichertes Geburtsjahr. */
    REGULAR;

    companion object {
        /**
         * Leitet das [BirthdayTier] aus dem nächsten Alter ab.
         *
         * @param nextAge Das nächste Alter des Kontakts, oder `null` wenn kein Geburtsjahr bekannt ist.
         */
        fun from(nextAge: Int?): BirthdayTier = when {
            nextAge != null && nextAge % 10 == 0 -> MILESTONE_GOLD
            nextAge != null && nextAge % 5 == 0 -> MILESTONE_SILVER
            nextAge != null && nextAge in 0..9 -> CHILD
            else -> REGULAR
        }
    }
}
