package com.heckmannch.birthdaybuddy.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `safeDaysUntilNext returns 0 if birthday is today`() {
        val birthday = LocalDate.of(1990, 5, 15)
        val today = LocalDate.of(2024, 5, 15)

        val days = birthday.safeDaysUntilNext(today)

        assertThat(days).isEqualTo(0)
    }

    @Test
    fun `safeDaysUntilNext handles leap year birthday correctly in non-leap year`() {
        val birthday = LocalDate.of(2000, 2, 29)
        val today = LocalDate.of(2023, 2, 28) // Non-leap year

        // In non-leap years, we treat Feb 29th as Feb 28th
        val days = birthday.safeDaysUntilNext(today)

        assertThat(days).isEqualTo(0)
    }

    @Test
    fun `safeNextAge calculates age correctly for next occurrence`() {
        val birthday = LocalDate.of(1990, 5, 15)
        val today = LocalDate.of(2024, 5, 14) // One day before 34th birthday

        val age = birthday.safeNextAge(today)

        assertThat(age).isEqualTo(34)
    }

    @Test
    fun `safeNextAge handles age after birthday correctly`() {
        val birthday = LocalDate.of(1990, 5, 15)
        val today = LocalDate.of(2024, 5, 16) // One day after 34th birthday

        val age = birthday.safeNextAge(today)

        assertThat(age).isEqualTo(35) // Next birthday is the 35th
    }

    @Test
    fun `isBirthdayToday returns true for current day`() {
        val birthday = LocalDate.of(1995, 10, 20)
        val today = LocalDate.of(2024, 10, 20)

        assertThat(birthday.isBirthdayToday(today)).isTrue()
    }

    @Test
    fun `hasYear returns false for NO_YEAR_MARKER`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 5, 15)
        assertThat(birthday.hasYear).isFalse()
    }

    @Test
    fun `safeNextAge returns null for contacts without year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 5, 15)
        assertThat(birthday.safeNextAge()).isNull()
    }

    @Test
    fun `mergeNames combines names with matching last names correctly`() {
        val merged = mergeNames("Max Mustermann", "Erika Mustermann")
        assertThat(merged).isEqualTo("Max & Erika Mustermann")
    }

    @Test
    fun `mergeNames combines names with different last names correctly`() {
        val merged = mergeNames("Max Schmidt", "Erika Mustermann")
        assertThat(merged).isEqualTo("Max Schmidt & Erika Mustermann")
    }

    @Test
    fun `mergeNames combines single names correctly`() {
        val merged = mergeNames("Max", "Erika")
        assertThat(merged).isEqualTo("Max & Erika")
    }
}
