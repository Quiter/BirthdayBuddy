package com.heckmannch.birthdaybuddy.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [PhoneNumberNormalizer].
 *
 * Verifies E.164 compliance across international, national, trunk-zero,
 * legacy-buggy, and multi-country phone number formats.
 */
class PhoneNumberNormalizerTest {

    // -------------------------------------------------------------------------
    // International formats with '+'
    // -------------------------------------------------------------------------

    @Test
    fun `normalize handles international number with leading plus and whitespace`() {
        val result = PhoneNumberNormalizer.normalize("+49 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize handles international number with dashes, dots, and slashes`() {
        assertThat(PhoneNumberNormalizer.normalize("+49-170-1234567")).isEqualTo("+491701234567")
        assertThat(PhoneNumberNormalizer.normalize("+49.170.1234567")).isEqualTo("+491701234567")
        assertThat(PhoneNumberNormalizer.normalize("+49/170/1234567")).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize handles parenthesized country code like (+49)`() {
        val result = PhoneNumberNormalizer.normalize("(+49) 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    // -------------------------------------------------------------------------
    // International formats with '00'
    // -------------------------------------------------------------------------

    @Test
    fun `normalize handles international number with leading 00`() {
        val result = PhoneNumberNormalizer.normalize("0049 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize handles international number with spaces between 00 and country code`() {
        val result = PhoneNumberNormalizer.normalize("00 49 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    // -------------------------------------------------------------------------
    // National numbers with leading trunk zero
    // -------------------------------------------------------------------------

    @Test
    fun `normalize handles standard national mobile number with leading 0`() {
        val result = PhoneNumberNormalizer.normalize("0170 1234567", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize handles national landline number with leading 0`() {
        assertThat(PhoneNumberNormalizer.normalize("0711 123 456", defaultCountryIso = "DE"))
            .isEqualTo("+49711123456")
        assertThat(PhoneNumberNormalizer.normalize("030 / 123456", defaultCountryIso = "DE"))
            .isEqualTo("+4930123456")
    }

    @Test
    fun `normalize handles parenthesized area code with dash`() {
        val result = PhoneNumberNormalizer.normalize("(0170) 123-456", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+49170123456")
    }

    // -------------------------------------------------------------------------
    // Redundant trunk zero in parentheses: +49 (0) 170...
    // -------------------------------------------------------------------------

    @Test
    fun `normalize removes redundant trunk zero in parentheses after plus prefix`() {
        val result = PhoneNumberNormalizer.normalize("+49 (0) 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize removes redundant trunk zero in brackets after plus prefix`() {
        val result = PhoneNumberNormalizer.normalize("+49 [0] 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize removes redundant trunk zero in parentheses without spaces`() {
        val result = PhoneNumberNormalizer.normalize("+49(0)1701234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize removes redundant trunk zero in parentheses after 00 prefix`() {
        val result = PhoneNumberNormalizer.normalize("0049 (0) 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    // -------------------------------------------------------------------------
    // Erroneous legacy prefixes (e.g. +0... or +00...)
    // -------------------------------------------------------------------------

    @Test
    fun `normalize fixes erroneous plus-zero prefix from legacy string concatenation`() {
        val result = PhoneNumberNormalizer.normalize("+0170 1234567", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize fixes erroneous plus-zero-zero prefix`() {
        val result = PhoneNumberNormalizer.normalize("+0049 170 1234567")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize fixes erroneous trunk zero typed immediately after country code`() {
        val result = PhoneNumberNormalizer.normalize("+49 0170 1234567", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+491701234567")
    }

    @Test
    fun `normalize fixes erroneous trunk zero after 00 country code`() {
        val result = PhoneNumberNormalizer.normalize("0049 0170 1234567", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+491701234567")
    }

    // -------------------------------------------------------------------------
    // Country ISO variations (AT, CH, US, IT)
    // -------------------------------------------------------------------------

    @Test
    fun `normalize resolves Austrian numbers correctly`() {
        val result = PhoneNumberNormalizer.normalize("0664 1234567", defaultCountryIso = "AT")
        assertThat(result).isEqualTo("+436641234567")
    }

    @Test
    fun `normalize resolves Swiss numbers correctly`() {
        val result = PhoneNumberNormalizer.normalize("079 123 45 67", defaultCountryIso = "CH")
        assertThat(result).isEqualTo("+41791234567")
    }

    @Test
    fun `normalize resolves US numbers correctly`() {
        val result = PhoneNumberNormalizer.normalize("(555) 234-5678", defaultCountryIso = "US")
        assertThat(result).isEqualTo("+15552345678")
    }

    @Test
    fun `normalize preserves foreign international number even if defaultCountryIso differs`() {
        val result = PhoneNumberNormalizer.normalize("+1 (555) 234-5678", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+15552345678")
    }

    @Test
    fun `normalize retains leading zero for Italian numbers according to Italian numbering plan`() {
        val result = PhoneNumberNormalizer.normalize("06 123456", defaultCountryIso = "IT")
        assertThat(result).isEqualTo("+3906123456")
    }

    // -------------------------------------------------------------------------
    // Numbers starting with calling code without '+'
    // -------------------------------------------------------------------------

    @Test
    fun `normalize handles number starting with calling code but missing plus`() {
        val result = PhoneNumberNormalizer.normalize("491701234567", defaultCountryIso = "DE")
        assertThat(result).isEqualTo("+491701234567")
    }

    // -------------------------------------------------------------------------
    // Blank, invalid, and short emergency numbers
    // -------------------------------------------------------------------------

    @Test
    fun `normalize returns empty string for blank or empty inputs`() {
        assertThat(PhoneNumberNormalizer.normalize("")).isEmpty()
        assertThat(PhoneNumberNormalizer.normalize("   ")).isEmpty()
        assertThat(PhoneNumberNormalizer.normalize(" \t\n ")).isEmpty()
    }

    @Test
    fun `normalize returns empty string when input has no digits`() {
        assertThat(PhoneNumberNormalizer.normalize("abc")).isEmpty()
        assertThat(PhoneNumberNormalizer.normalize("(-/.)")).isEmpty()
    }

    @Test
    fun `normalize preserves short emergency numbers without prepending country code`() {
        assertThat(PhoneNumberNormalizer.normalize("112")).isEqualTo("112")
        assertThat(PhoneNumberNormalizer.normalize("110")).isEqualTo("110")
        assertThat(PhoneNumberNormalizer.normalize("911")).isEqualTo("911")
    }

    // -------------------------------------------------------------------------
    // WhatsApp digits-only format (normalizeToDigitsOnly)
    // -------------------------------------------------------------------------

    @Test
    fun `normalizeToDigitsOnly strips leading plus for WhatsApp API compatibility`() {
        assertThat(PhoneNumberNormalizer.normalizeToDigitsOnly("+49 170 1234567"))
            .isEqualTo("491701234567")
        assertThat(PhoneNumberNormalizer.normalizeToDigitsOnly("0170 1234567", defaultCountryIso = "DE"))
            .isEqualTo("491701234567")
        assertThat(PhoneNumberNormalizer.normalizeToDigitsOnly("(0170) 123-456", defaultCountryIso = "DE"))
            .isEqualTo("49170123456")
        assertThat(PhoneNumberNormalizer.normalizeToDigitsOnly("")).isEmpty()
    }

    // -------------------------------------------------------------------------
    // getCountryCallingCode
    // -------------------------------------------------------------------------

    @Test
    fun `getCountryCallingCode resolves uppercase and lowercase ISO codes`() {
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("DE")).isEqualTo("49")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("de")).isEqualTo("49")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("AT")).isEqualTo("43")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("CH")).isEqualTo("41")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("US")).isEqualTo("1")
    }

    @Test
    fun `getCountryCallingCode falls back to 49 for unknown or blank inputs`() {
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("")).isEqualTo("49")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("UNKNOWN")).isEqualTo("49")
    }

    @Test
    fun `getCountryCallingCode returns digits directly if numeric calling code is passed`() {
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("49")).isEqualTo("49")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("+49")).isEqualTo("49")
        assertThat(PhoneNumberNormalizer.getCountryCallingCode("+1")).isEqualTo("1")
    }
}
