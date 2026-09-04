package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab
import kotlinx.serialization.json.Json
import org.junit.Test

class NavRoutesTest {

    @Test
    fun `Settings is a valid NavKey with default and custom initialTab`() {
        val defaultSettings: NavKey = Settings()
        val customSettings: NavKey = Settings(initialTab = SettingsTab.NOTIFICATIONS)

        assertThat((defaultSettings as Settings).initialTab).isNull()
        assertThat((customSettings as Settings).initialTab).isEqualTo(SettingsTab.NOTIFICATIONS)
    }

    @Test
    fun `NavKey entries exist for all routes`() {
        val defaultSettings = Settings()
        val keys: List<NavKey> = listOf(Home, Onboarding, defaultSettings)
        assertThat(keys).containsExactly(Home, Onboarding, defaultSettings)
    }

    @Test
    fun `Settings serializes and deserializes correctly`() {
        val defaultSettings = Settings()
        val encodedDefault = Json.encodeToString(defaultSettings)
        val decodedDefault = Json.decodeFromString<Settings>(encodedDefault)
        assertThat(decodedDefault).isEqualTo(defaultSettings)
        assertThat(decodedDefault.initialTab).isNull()

        val notificationSettings = Settings(initialTab = SettingsTab.NOTIFICATIONS)
        val encodedCustom = Json.encodeToString(notificationSettings)
        val decodedCustom = Json.decodeFromString<Settings>(encodedCustom)
        assertThat(decodedCustom).isEqualTo(notificationSettings)
        assertThat(decodedCustom.initialTab).isEqualTo(SettingsTab.NOTIFICATIONS)
    }
}
