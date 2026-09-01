package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavRoutesTest {

    @Test
    fun `NotificationSettings is a valid NavKey`() {
        val key: NavKey = NotificationSettings
        assertThat(key).isEqualTo(NotificationSettings)
    }

    @Test
    fun `NavKey entries exist for all routes including NotificationSettings`() {
        val keys: List<NavKey> = listOf(Home, Onboarding, Settings, NotificationSettings)
        assertThat(keys).containsExactly(Home, Onboarding, Settings, NotificationSettings)
    }
}
