package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import org.junit.Test

/**
 * Unit tests for [AppSettingsMapper].
 */
class AppSettingsMapperTest {

    private val mapper = AppSettingsMapper()

    @Test
    fun toDomain_mapsAllFieldsAndStandardAccentCorrectly() {
        val entity = AppSettingsEntity(
            id = 0,
            notificationsEnabled = true,
            persistentNotifications = false,
            onboardingCompleted = true,
            lastSyncTimestamp = 123456789L,
            calendarSyncEnabled = true,
            calendarId = 1L,
            otherEventsEnabled = true,
            ignoredCouplePairs = listOf("1_2", "3_4"),
            birthdayCalendarColor = 0xFF112233.toInt(),
            anniversaryCalendarColor = 0xFF445566.toInt(),
            nameDayCalendarColor = 0xFF778899.toInt(),
            themeMode = ThemeMode.DARK,
            themeAmoled = true,
            themeAccent = "BLUE",
            labelsEnabled = false
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.id).isEqualTo(0)
        assertThat(domain.notificationsEnabled).isTrue()
        assertThat(domain.persistentNotifications).isFalse()
        assertThat(domain.onboardingCompleted).isTrue()
        assertThat(domain.lastSyncTimestamp).isEqualTo(123456789L)
        assertThat(domain.calendarSyncEnabled).isTrue()
        assertThat(domain.calendarId).isEqualTo(1L)
        assertThat(domain.otherEventsEnabled).isTrue()
        assertThat(domain.ignoredCouplePairs).containsExactly("1_2", "3_4").inOrder()
        assertThat(domain.birthdayCalendarColor).isEqualTo(0xFF112233.toInt())
        assertThat(domain.anniversaryCalendarColor).isEqualTo(0xFF445566.toInt())
        assertThat(domain.nameDayCalendarColor).isEqualTo(0xFF778899.toInt())
        assertThat(domain.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(domain.themeAmoled).isTrue()
        assertThat(domain.themeAccent).isEqualTo(ThemeAccent.BLUE)
        assertThat(domain.customAccentColor).isNull()
        assertThat(domain.labelsEnabled).isFalse()
    }

    @Test
    fun toDomain_mapsCustomHexAccentCorrectly() {
        val entity = AppSettingsEntity(
            themeAccent = "#FF5722"
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.themeAccent).isEqualTo(ThemeAccent.CUSTOM)
        assertThat(domain.customAccentColor).isEqualTo("#FF5722")
    }

    @Test
    fun toDomain_fallsBackToSystemOnInvalidAccent() {
        val entity = AppSettingsEntity(
            themeAccent = "INVALID_ACCENT_NAME"
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.themeAccent).isEqualTo(ThemeAccent.SYSTEM)
        assertThat(domain.customAccentColor).isNull()
    }

    @Test
    fun toEntity_mapsAllFieldsAndStandardAccentCorrectly() {
        val domain = AppSettings(
            id = 0,
            notificationsEnabled = true,
            persistentNotifications = false,
            onboardingCompleted = true,
            lastSyncTimestamp = 123456789L,
            calendarSyncEnabled = true,
            calendarId = 1L,
            otherEventsEnabled = true,
            ignoredCouplePairs = listOf("1_2", "3_4"),
            birthdayCalendarColor = 0xFF112233.toInt(),
            anniversaryCalendarColor = 0xFF445566.toInt(),
            nameDayCalendarColor = 0xFF778899.toInt(),
            themeMode = ThemeMode.DARK,
            themeAmoled = true,
            themeAccent = ThemeAccent.BLUE,
            customAccentColor = null,
            labelsEnabled = false
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.id).isEqualTo(0)
        assertThat(entity.notificationsEnabled).isTrue()
        assertThat(entity.persistentNotifications).isFalse()
        assertThat(entity.onboardingCompleted).isTrue()
        assertThat(entity.lastSyncTimestamp).isEqualTo(123456789L)
        assertThat(entity.calendarSyncEnabled).isTrue()
        assertThat(entity.calendarId).isEqualTo(1L)
        assertThat(entity.otherEventsEnabled).isTrue()
        assertThat(entity.ignoredCouplePairs).containsExactly("1_2", "3_4").inOrder()
        assertThat(entity.birthdayCalendarColor).isEqualTo(0xFF112233.toInt())
        assertThat(entity.anniversaryCalendarColor).isEqualTo(0xFF445566.toInt())
        assertThat(entity.nameDayCalendarColor).isEqualTo(0xFF778899.toInt())
        assertThat(entity.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(entity.themeAmoled).isTrue()
        assertThat(entity.themeAccent).isEqualTo("BLUE")
        assertThat(entity.labelsEnabled).isFalse()
    }

    @Test
    fun toEntity_mapsCustomAccentWithValidHexColor() {
        val domain = AppSettings(
            themeAccent = ThemeAccent.CUSTOM,
            customAccentColor = "#FF5722"
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.themeAccent).isEqualTo("#FF5722")
    }

    @Test
    fun toEntity_mapsCustomAccentWithEmptyColorFallsBackToEnumName() {
        val domain = AppSettings(
            themeAccent = ThemeAccent.CUSTOM,
            customAccentColor = ""
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.themeAccent).isEqualTo("CUSTOM")
    }

    @Test
    fun toEntity_mapsCustomAccentWithNullColorFallsBackToEnumName() {
        val domain = AppSettings(
            themeAccent = ThemeAccent.CUSTOM,
            customAccentColor = null
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.themeAccent).isEqualTo("CUSTOM")
    }
}
