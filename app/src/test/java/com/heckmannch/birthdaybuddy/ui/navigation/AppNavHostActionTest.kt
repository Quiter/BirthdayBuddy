package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeIntent
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppNavHostActionTest {

    // -------------------------------------------------------------------------
    // handleGlobalAppAction – Onboarding guard
    // -------------------------------------------------------------------------

    @Test
    fun `handleGlobalAppAction ignores NavigateToNotifications while Onboarding is active`() {
        val backStack = mutableListOf<NavKey>(Onboarding)
        var handled = false

        handleGlobalAppAction(
            action = AppAction.NavigateToNotifications,
            backStack = backStack,
            onActionHandled = { handled = true },
        )

        assertThat(backStack).containsExactly(Onboarding)
        assertThat(handled).isTrue()
    }

    @Test
    fun `handleGlobalAppAction ignores Home deep links while Onboarding is active`() {
        val homeActions = listOf(
            AppAction.OpenSearch,
            AppAction.ScrollToTop,
            AppAction.OpenAddContact,
            AppAction.OpenBirthdayPicker("lookup_1", 1990, 5, 20),
        )

        homeActions.forEach { action ->
            val backStack = mutableListOf<NavKey>(Onboarding)
            var handled = false

            handleGlobalAppAction(
                action = action,
                backStack = backStack,
                onActionHandled = { handled = true },
            )

            assertThat(backStack).containsExactly(Onboarding)
            assertThat(handled).isTrue()
        }
    }

    // -------------------------------------------------------------------------
    // handleGlobalAppAction – Normal navigation when Onboarding is complete
    // -------------------------------------------------------------------------

    @Test
    fun `handleGlobalAppAction navigates to notification settings from Home`() {
        val backStack = mutableListOf<NavKey>(Home)
        var handled = false

        handleGlobalAppAction(
            action = AppAction.NavigateToNotifications,
            backStack = backStack,
            onActionHandled = { handled = true },
        )

        assertThat(backStack)
            .containsExactly(Home, Settings(initialTab = SettingsTab.NOTIFICATIONS))
            .inOrder()
        assertThat(handled).isTrue()
    }

    @Test
    fun `handleGlobalAppAction does not duplicate Settings key on repeated NavigateToNotifications`() {
        val target = Settings(initialTab = SettingsTab.NOTIFICATIONS)
        val backStack = mutableListOf<NavKey>(Home, target)
        var handled = false

        handleGlobalAppAction(
            action = AppAction.NavigateToNotifications,
            backStack = backStack,
            onActionHandled = { handled = true },
        )

        assertThat(backStack).containsExactly(Home, target).inOrder()
        assertThat(handled).isTrue()
    }

    @Test
    fun `handleGlobalAppAction returns to Home from Settings for Home actions without consuming`() {
        val backStack = mutableListOf<NavKey>(Home, Settings(initialTab = SettingsTab.NOTIFICATIONS))
        var handled = false

        handleGlobalAppAction(
            action = AppAction.OpenSearch,
            backStack = backStack,
            onActionHandled = { handled = true },
        )

        assertThat(backStack).containsExactly(Home)
        assertThat(handled).isFalse()
    }

    // -------------------------------------------------------------------------
    // handleHomeAppAction – Pager switch before HomeIntent dispatch
    // -------------------------------------------------------------------------

    @Test
    fun `handleHomeAppAction switches to Home page before OpenSearch intent`() = runTest {
        val callOrder = mutableListOf<String>()

        handleHomeAppAction(
            action = AppAction.OpenSearch,
            ensureHomePage = { callOrder.add("ensureHomePage") },
            onIntent = { intent ->
                assertThat(intent).isEqualTo(HomeIntent.TriggerSearchFocus)
                callOrder.add("onIntent")
            },
            onActionHandled = { callOrder.add("onActionHandled") },
        )

        assertThat(callOrder).containsExactly("ensureHomePage", "onIntent", "onActionHandled").inOrder()
    }

    @Test
    fun `handleHomeAppAction switches to Home page before ScrollToTop intent`() = runTest {
        val callOrder = mutableListOf<String>()

        handleHomeAppAction(
            action = AppAction.ScrollToTop,
            ensureHomePage = { callOrder.add("ensureHomePage") },
            onIntent = { intent ->
                assertThat(intent).isEqualTo(HomeIntent.TriggerScrollToTop)
                callOrder.add("onIntent")
            },
            onActionHandled = { callOrder.add("onActionHandled") },
        )

        assertThat(callOrder).containsExactly("ensureHomePage", "onIntent", "onActionHandled").inOrder()
    }

    @Test
    fun `handleHomeAppAction switches to Home page before OpenBirthdayPicker intent`() = runTest {
        val callOrder = mutableListOf<String>()
        val action = AppAction.OpenBirthdayPicker(
            contactLookupKey = "lookup_42",
            year = 1995,
            month = 7,
            day = 12,
        )

        handleHomeAppAction(
            action = action,
            ensureHomePage = { callOrder.add("ensureHomePage") },
            onIntent = { intent ->
                assertThat(intent).isEqualTo(
                    HomeIntent.OpenBirthdayPicker(
                        contactLookupKey = "lookup_42",
                        year = 1995,
                        month = 7,
                        day = 12,
                    )
                )
                callOrder.add("onIntent")
            },
            onActionHandled = { callOrder.add("onActionHandled") },
        )

        assertThat(callOrder).containsExactly("ensureHomePage", "onIntent", "onActionHandled").inOrder()
    }

    @Test
    fun `handleHomeAppAction dispatches SyncContacts for OpenAddContact`() = runTest {
        val dispatchedIntents = mutableListOf<HomeIntent>()
        var handled = false

        handleHomeAppAction(
            action = AppAction.OpenAddContact,
            ensureHomePage = { error("ensureHomePage should not be called for OpenAddContact") },
            onIntent = { dispatchedIntents.add(it) },
            onActionHandled = { handled = true },
        )

        assertThat(dispatchedIntents).containsExactly(HomeIntent.SyncContacts())
        assertThat(handled).isTrue()
    }

    @Test
    fun `handleHomeAppAction ignores NavigateToNotifications`() = runTest {
        var anyCalled = false

        handleHomeAppAction(
            action = AppAction.NavigateToNotifications,
            ensureHomePage = { anyCalled = true },
            onIntent = { anyCalled = true },
            onActionHandled = { anyCalled = true },
        )

        assertThat(anyCalled).isFalse()
    }
}
