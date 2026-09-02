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
    fun `hasYear returns false for NO_YEAR_MARKER on February 29`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 2, 29)
        assertThat(birthday.hasYear).isFalse()
    }

    @Test
    fun `toNextOccurrence handles February 29 without year in leap year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 2, 29)
        val today = LocalDate.of(2024, 1, 15) // 2024 is a leap year

        val nextOccurrence = birthday.toNextOccurrence(today)

        assertThat(nextOccurrence).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `toNextOccurrence handles February 29 without year in non-leap year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 2, 29)
        val today = LocalDate.of(2023, 1, 15) // 2023 is not a leap year

        val nextOccurrence = birthday.toNextOccurrence(today)

        // In non-leap years, February 29 falls back to February 28
        assertThat(nextOccurrence).isEqualTo(LocalDate.of(2023, 2, 28))
    }

    @Test
    fun `safeDaysUntilNext handles February 29 without year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 2, 29)
        val today = LocalDate.of(2024, 2, 28) // 2024 is a leap year, 1 day before Feb 29

        assertThat(birthday.safeDaysUntilNext(today)).isEqualTo(1)
    }

    @Test
    fun `safeNextAge returns null for contacts without year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 5, 15)
        assertThat(birthday.safeNextAge()).isNull()
    }

    @Test
    fun `safeNextAge returns null for February 29 without year`() {
        val birthday = LocalDate.of(NO_YEAR_MARKER, 2, 29)
        assertThat(birthday.safeNextAge()).isNull()
    }

    // --- sanitizeBirthdayDate Tests ---

    @Test
    fun `sanitizeBirthdayDate with normal valid date returns exact LocalDate`() {
        val result = sanitizeBirthdayDate(1992, 7, 24)
        assertThat(result).isEqualTo(LocalDate.of(1992, 7, 24))
    }

    @Test
    fun `sanitizeBirthdayDate clamps month smaller than 1 to January`() {
        val resultZero = sanitizeBirthdayDate(1990, 0, 15)
        assertThat(resultZero).isEqualTo(LocalDate.of(1990, 1, 15))

        val resultNegative = sanitizeBirthdayDate(1990, -5, 15)
        assertThat(resultNegative).isEqualTo(LocalDate.of(1990, 1, 15))
    }

    @Test
    fun `sanitizeBirthdayDate clamps month greater than 12 to December`() {
        val resultThirteen = sanitizeBirthdayDate(1990, 13, 25)
        assertThat(resultThirteen).isEqualTo(LocalDate.of(1990, 12, 25))

        val resultLargeMonth = sanitizeBirthdayDate(1990, 99, 25)
        assertThat(resultLargeMonth).isEqualTo(LocalDate.of(1990, 12, 25))
    }

    @Test
    fun `sanitizeBirthdayDate clamps day smaller than 1 to first day of month`() {
        val resultZero = sanitizeBirthdayDate(1995, 6, 0)
        assertThat(resultZero).isEqualTo(LocalDate.of(1995, 6, 1))

        val resultNegative = sanitizeBirthdayDate(1995, 6, -10)
        assertThat(resultNegative).isEqualTo(LocalDate.of(1995, 6, 1))
    }

    @Test
    fun `sanitizeBirthdayDate clamps day greater than 31 in 31-day month to 31`() {
        val result = sanitizeBirthdayDate(2000, 1, 35)
        assertThat(result).isEqualTo(LocalDate.of(2000, 1, 31))
    }

    @Test
    fun `sanitizeBirthdayDate clamps day greater than 30 in 30-day month to 30`() {
        val resultApril = sanitizeBirthdayDate(2001, 4, 31)
        assertThat(resultApril).isEqualTo(LocalDate.of(2001, 4, 30))

        val resultNov = sanitizeBirthdayDate(2001, 11, 40)
        assertThat(resultNov).isEqualTo(LocalDate.of(2001, 11, 30))
    }

    @Test
    fun `sanitizeBirthdayDate clamps day greater than 28 in non-leap year February to 28`() {
        val result29 = sanitizeBirthdayDate(2023, 2, 29)
        assertThat(result29).isEqualTo(LocalDate.of(2023, 2, 28))

        val result30 = sanitizeBirthdayDate(2023, 2, 30)
        assertThat(result30).isEqualTo(LocalDate.of(2023, 2, 28))
    }

    @Test
    fun `sanitizeBirthdayDate allows February 29 in leap year`() {
        val resultLeap = sanitizeBirthdayDate(2024, 2, 29)
        assertThat(resultLeap).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `sanitizeBirthdayDate clamps day greater than 29 in leap year February to 29`() {
        val resultLeap30 = sanitizeBirthdayDate(2024, 2, 30)
        assertThat(resultLeap30).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun `sanitizeBirthdayDate with null year sets targetYear to NO_YEAR_MARKER`() {
        val result = sanitizeBirthdayDate(null, 8, 18)
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 8, 18))
        assertThat(result.hasYear).isFalse()
    }

    @Test
    fun `sanitizeBirthdayDate with NO_YEAR_MARKER preserves NO_YEAR_MARKER and supports February 29`() {
        val result = sanitizeBirthdayDate(NO_YEAR_MARKER, 2, 29)
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
        assertThat(result.hasYear).isFalse()

        val resultOverflow = sanitizeBirthdayDate(NO_YEAR_MARKER, 2, 31)
        assertThat(resultOverflow).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
    }

    @Test
    fun `sanitizeBirthdayDate with non-positive year falls back to NO_YEAR_MARKER`() {
        val resultZeroYear = sanitizeBirthdayDate(0, 3, 10)
        assertThat(resultZeroYear).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 3, 10))

        val resultNegativeYear = sanitizeBirthdayDate(-1990, 3, 10)
        assertThat(resultNegativeYear).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 3, 10))
    }
}
