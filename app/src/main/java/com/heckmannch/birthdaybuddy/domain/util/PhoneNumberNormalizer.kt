package com.heckmannch.birthdaybuddy.domain.util

import java.util.Locale

/**
 * Pure domain utility for normalizing phone numbers to the E.164 standard.
 *
 * This utility runs purely on the JVM without any Android SDK dependencies (`android.telephony.*`),
 * ensuring full compatibility with Clean Architecture domain boundaries and fast JVM unit testing.
 *
 * Supported workflows and edge cases:
 * - Local numbers with leading trunk zero (e.g. `0170 1234567`, `(0170) 123-456`) -> `+491701234567`.
 * - International numbers with `+` (e.g. `+49 170 1234567`) -> `+491701234567`.
 * - International numbers with `00` prefix (e.g. `0049 170 1234567`) -> `+491701234567`.
 * - Redundant trunk zero in parentheses (e.g. `+49 (0) 170 1234567`, `0049 (0) 170 1234567`) -> `+491701234567`.
 * - Erroneous legacy `+0...` prefix caused by faulty string concatenation -> `+491701234567`.
 * - Country-specific handling (e.g. Italian numbers retaining leading 0 in E.164).
 * - Formatting for third-party messengers like WhatsApp (digits-only E.164 via [normalizeToDigitsOnly]).
 */
object PhoneNumberNormalizer {

    private const val DEFAULT_COUNTRY_ISO = "DE"
    private const val DEFAULT_CALLING_CODE = "49"

    /**
     * Matches redundant trunk zeros enclosed in parentheses or brackets, such as `(0)`, `( 0 )`, `[0]`.
     */
    private val TRUNK_ZERO_REGEX = """[\(\[]\s*[0oO]\s*[\)\]]""".toRegex()

    /**
     * Mapping of ISO 3166-1 alpha-2 country codes to ITU-T international calling codes.
     */
    private val COUNTRY_CALLING_CODES: Map<String, String> = mapOf(
        // DACH & Neighbouring Countries
        "DE" to "49",
        "AT" to "43",
        "CH" to "41",
        "LI" to "423",
        "LU" to "352",
        "FR" to "33",
        "NL" to "31",
        "BE" to "32",
        "DK" to "45",
        "PL" to "48",
        "CZ" to "420",
        "SK" to "421",
        "IT" to "39",
        // Rest of Europe
        "GB" to "44",
        "UK" to "44",
        "IE" to "353",
        "ES" to "34",
        "PT" to "351",
        "SE" to "46",
        "NO" to "47",
        "FI" to "358",
        "GR" to "30",
        "HR" to "385",
        "SI" to "386",
        "HU" to "36",
        "RO" to "40",
        "BG" to "359",
        "UA" to "380",
        "TR" to "90",
        "IS" to "354",
        "EE" to "372",
        "LV" to "371",
        "LT" to "370",
        "CY" to "357",
        "MT" to "356",
        // Americas
        "US" to "1",
        "CA" to "1",
        "MX" to "52",
        "BR" to "55",
        "AR" to "54",
        "CL" to "56",
        "CO" to "57",
        // Asia / Pacific / Africa
        "AU" to "61",
        "NZ" to "64",
        "JP" to "81",
        "CN" to "86",
        "IN" to "91",
        "KR" to "82",
        "SG" to "65",
        "HK" to "852",
        "TW" to "886",
        "TH" to "66",
        "VN" to "84",
        "ID" to "62",
        "MY" to "60",
        "PH" to "63",
        "IL" to "972",
        "AE" to "971",
        "SA" to "966",
        "EG" to "20",
        "ZA" to "27",
        "RU" to "7",
    )

    /**
     * Resolves the international calling code for the provided [countryIso] or calling code string.
     * Defaults to `"49"` (Germany) if unrecognized or blank.
     */
    fun getCountryCallingCode(countryIso: String): String {
        val clean = countryIso.trim().removePrefix("+")
        if (clean.isNotEmpty() && clean.all { it.isDigit() }) {
            return clean
        }
        val iso = clean.uppercase()
        return COUNTRY_CALLING_CODES[iso] ?: DEFAULT_CALLING_CODE
    }

    /**
     * Normalizes a phone number to standard E.164 format (e.g. `+491701234567`).
     *
     * @param phoneNumber The raw input phone number string.
     * @param defaultCountryIso The 2-letter ISO country code used for resolving national numbers.
     *   Defaults to the system's default country ([Locale.getDefault].country).
     * @return The cleaned and standardized E.164 phone number, or an empty string if invalid/empty.
     */
    fun normalize(
        phoneNumber: String,
        defaultCountryIso: String = Locale.getDefault().country
    ): String {
        if (phoneNumber.isBlank()) return ""

        val effectiveIso = defaultCountryIso.ifBlank { DEFAULT_COUNTRY_ISO }
        val callingCode = getCountryCallingCode(effectiveIso)

        // 1. Strip redundant trunk zero in parentheses or brackets, e.g. "+49 (0) 170..." -> "+49  170..."
        val text = phoneNumber.replace(TRUNK_ZERO_REGEX, " ").trim()

        // 2. Determine whether a '+' precedes the first digit
        val firstDigitIndex = text.indexOfFirst { it.isDigit() }
        if (firstDigitIndex == -1) return ""

        val prefixBeforeDigit = text.substring(0, firstDigitIndex)
        val hasLeadingPlus = prefixBeforeDigit.contains('+')

        // 3. Extract all digits
        val digitsOnly = buildString {
            for (i in firstDigitIndex until text.length) {
                val c = text[i]
                if (c.isDigit()) append(c)
            }
        }

        if (digitsOnly.isEmpty()) return ""

        // Handle short numbers (e.g. emergency numbers like 110, 112, 911)
        if (!hasLeadingPlus && digitsOnly.length <= 4) {
            return digitsOnly
        }

        // 4. Construct E.164 formatted string
        return when {
            // Erroneous legacy "+00..." -> "+..."
            hasLeadingPlus && digitsOnly.startsWith("00") -> {
                "+" + digitsOnly.trimStart('0')
            }

            // Erroneous legacy "+0..." -> strip leading '0' and prepend standard calling code
            hasLeadingPlus && digitsOnly.startsWith("0") -> {
                val stripped = digitsOnly.trimStart('0')
                "+$callingCode$stripped"
            }

            // Standard international with leading '+'
            hasLeadingPlus -> {
                // If starts with callingCode (e.g. 49) followed by an erroneous '0' (except Italy +39)
                if (callingCode != "39" && digitsOnly.startsWith(callingCode + "0")) {
                    val afterCallingCode = digitsOnly.substring(callingCode.length).trimStart('0')
                    "+$callingCode$afterCallingCode"
                } else {
                    "+$digitsOnly"
                }
            }

            // International with "00..." prefix (e.g. "0049 170...")
            digitsOnly.startsWith("00") -> {
                val after00 = digitsOnly.substring(2)
                if (callingCode != "39" && after00.startsWith(callingCode + "0")) {
                    val afterCallingCode = after00.substring(callingCode.length).trimStart('0')
                    "+$callingCode$afterCallingCode"
                } else {
                    "+$after00"
                }
            }

            // National number starting with single '0' (e.g. "0170 1234567" or "(0170) 123-456")
            digitsOnly.startsWith("0") -> {
                if (effectiveIso.equals("IT", ignoreCase = true)) {
                    "+39$digitsOnly"
                } else {
                    val stripped = digitsOnly.trimStart('0')
                    "+$callingCode$stripped"
                }
            }

            // Number already starts with calling code and has realistic length (>= callingCode.length + 6)
            digitsOnly.startsWith(callingCode) && digitsOnly.length >= callingCode.length + 6 -> {
                "+$digitsOnly"
            }

            // Otherwise treat as local number missing the trunk prefix
            else -> {
                "+$callingCode$digitsOnly"
            }
        }
    }

    /**
     * Normalizes a phone number to digits-only E.164 representation (without leading `+`).
     *
     * This format is required by messenger APIs such as WhatsApp (`https://wa.me/<digits>`).
     */
    fun normalizeToDigitsOnly(
        phoneNumber: String,
        defaultCountryIso: String = Locale.getDefault().country
    ): String = normalize(phoneNumber, defaultCountryIso).removePrefix("+")
}
