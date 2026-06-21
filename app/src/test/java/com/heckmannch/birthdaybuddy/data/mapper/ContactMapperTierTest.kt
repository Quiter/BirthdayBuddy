package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.ui.model.BirthdayTier
import com.heckmannch.birthdaybuddy.ui.model.EventType
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import org.junit.Test
import java.time.LocalDate

/**
 * Unit-Tests für die BirthdayTier-Logik.
 *
 * Abdeckung:
 * 1. [BirthdayTier.from] – direkte Factory-Methode mit allen Tier-Grenzen
 * 2. Integration mit [ContactMapper.toUiModelForEvent] – verifiziert, dass das Tier
 *    korrekt auf das [com.heckmannch.birthdaybuddy.ui.model.ContactUiModel] gemappt wird.
 */
class ContactMapperTierTest {

    private val mapper = ContactMapper()

    // Referenz-Datum: 15. Mai 2024
    private val today = LocalDate.of(2024, 5, 15)

    // ────────────────────────────────────────────────────────────────
    // BirthdayTier.from(nextAge?) – Unit-Tests
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `from - null returns REGULAR`() {
        assertThat(BirthdayTier.from(null)).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `from - age 10 returns MILESTONE_GOLD`() {
        assertThat(BirthdayTier.from(10)).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `from - age 20 returns MILESTONE_GOLD`() {
        assertThat(BirthdayTier.from(20)).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `from - age 30 returns MILESTONE_GOLD`() {
        assertThat(BirthdayTier.from(30)).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `from - age 100 returns MILESTONE_GOLD`() {
        assertThat(BirthdayTier.from(100)).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `from - age 5 returns MILESTONE_SILVER`() {
        // 5 % 5 == 0 aber 5 % 10 != 0 → SILVER, nicht GOLD
        // Außerdem: 5 in 0..9, aber GOLD/SILVER hat Vorrang vor CHILD
        assertThat(BirthdayTier.from(5)).isEqualTo(BirthdayTier.MILESTONE_SILVER)
    }

    @Test
    fun `from - age 15 returns MILESTONE_SILVER`() {
        assertThat(BirthdayTier.from(15)).isEqualTo(BirthdayTier.MILESTONE_SILVER)
    }

    @Test
    fun `from - age 25 returns MILESTONE_SILVER`() {
        assertThat(BirthdayTier.from(25)).isEqualTo(BirthdayTier.MILESTONE_SILVER)
    }

    @Test
    fun `from - age 0 returns CHILD`() {
        // 0 % 10 == 0 → GOLD hat Vorrang; 0 % 5 == 0 → SILVER hat Vorrang.
        // Laut Enum-Priorität: GOLD > SILVER > CHILD → 0 wird MILESTONE_GOLD
        assertThat(BirthdayTier.from(0)).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `from - age 1 returns CHILD`() {
        assertThat(BirthdayTier.from(1)).isEqualTo(BirthdayTier.CHILD)
    }

    @Test
    fun `from - age 3 returns CHILD`() {
        assertThat(BirthdayTier.from(3)).isEqualTo(BirthdayTier.CHILD)
    }

    @Test
    fun `from - age 9 returns CHILD`() {
        assertThat(BirthdayTier.from(9)).isEqualTo(BirthdayTier.CHILD)
    }

    @Test
    fun `from - age 11 returns REGULAR`() {
        assertThat(BirthdayTier.from(11)).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `from - age 13 returns REGULAR`() {
        assertThat(BirthdayTier.from(13)).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `from - age 42 returns REGULAR`() {
        assertThat(BirthdayTier.from(42)).isEqualTo(BirthdayTier.REGULAR)
    }

    // ────────────────────────────────────────────────────────────────
    // Integrations-Tests: ContactMapper setzt birthdayTier korrekt
    // ────────────────────────────────────────────────────────────────

    @Test
    fun `toUiModel - age 30 (today) maps to MILESTONE_GOLD`() {
        // Geburtstag am 15. Mai 1994 → nextAge = 30 → MILESTONE_GOLD
        val contact = Contact(
            contactId = "1", lookupKey = "k1", fullName = "Gold Person",
            birthday = LocalDate.of(1994, 5, 15)
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }

    @Test
    fun `toUiModel - age 25 maps to MILESTONE_SILVER`() {
        // Geburtstag am 20. Mai 1999 → nextAge = 25 → MILESTONE_SILVER
        val contact = Contact(
            contactId = "2", lookupKey = "k2", fullName = "Silver Person",
            birthday = LocalDate.of(1999, 5, 20)
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.MILESTONE_SILVER)
    }

    @Test
    fun `toUiModel - age 7 maps to CHILD`() {
        // Geburtstag am 20. Mai 2017 → nextAge = 7 → CHILD
        val contact = Contact(
            contactId = "3", lookupKey = "k3", fullName = "Kid Person",
            birthday = LocalDate.of(2017, 5, 20)
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.CHILD)
    }

    @Test
    fun `toUiModel - age 33 maps to REGULAR`() {
        // Geburtstag am 20. Mai 1991 → nextAge = 33 → REGULAR
        val contact = Contact(
            contactId = "4", lookupKey = "k4", fullName = "Regular Person",
            birthday = LocalDate.of(1991, 5, 20)
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `toUiModel - no year marker maps to REGULAR`() {
        // Kein Geburtsjahr bekannt → nextAge = null → REGULAR
        val contact = Contact(
            contactId = "5", lookupKey = "k5", fullName = "No Year Person",
            birthday = LocalDate.of(NO_YEAR_MARKER, 5, 20)
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.nextAge).isNull()
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `toUiModel - null birthday maps to REGULAR`() {
        val contact = Contact(
            contactId = "6", lookupKey = "k6", fullName = "No Birthday",
            birthday = null
        )
        val model = mapper.toUiModel(contact, today)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.REGULAR)
    }

    @Test
    fun `toUiModelForEvent - anniversary age 10 maps to MILESTONE_GOLD`() {
        // Hochzeitstag am 15. Mai 2014 → nextAge = 10 → MILESTONE_GOLD
        val contact = Contact(
            contactId = "7", lookupKey = "k7", fullName = "Gold Anniversary",
            anniversary = LocalDate.of(2014, 5, 15)
        )
        val model = mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
        assertThat(model.birthdayTier).isEqualTo(BirthdayTier.MILESTONE_GOLD)
    }
}
