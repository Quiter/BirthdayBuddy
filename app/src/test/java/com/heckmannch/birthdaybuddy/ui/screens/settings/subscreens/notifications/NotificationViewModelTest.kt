package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository: NotificationRepository = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val permissionChecker: PermissionChecker = mock()

    private lateinit var viewModel: NotificationViewModel

    private val testSettings = AppSettings(
        notificationsEnabled = true,
        persistentNotifications = false,
    )

    private val testRules = listOf(
        NotificationRule(daysBefore = 0, hour = 9, minute = 0),
        NotificationRule(daysBefore = 1, hour = 18, minute = 0)
    )

    @Before
    fun setup() = runTest {
        whenever(settingsRepository.settings).thenReturn(flowOf(testSettings))
        whenever(notificationRepository.allRules).thenReturn(flowOf(testRules))
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(testRules)
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)

        viewModel = NotificationViewModel(notificationRepository, settingsRepository, permissionChecker)
    }

    @Test
    fun `uiState should reflect data from repositories`() = runTest {
        val uiState = viewModel.uiState.first { it.notificationRules.isNotEmpty() }

        assertThat(uiState.notificationsEnabled).isTrue()
        assertThat(uiState.persistentNotifications).isFalse()
        assertThat(uiState.notificationRules).isEqualTo(testRules)
        assertThat(uiState.hasSystemNotificationPermission).isTrue()
    }

    @Test
    fun `refreshPermissionStatus should update uiState hasSystemNotificationPermission`() = runTest {
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        val viewModel = NotificationViewModel(
            notificationRepository,
            settingsRepository,
            permissionChecker
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        runCurrent()
        assertThat(viewModel.uiState.value.hasSystemNotificationPermission).isFalse()

        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        viewModel.onIntent(NotificationIntent.RefreshPermissionStatus)
        runCurrent()

        assertThat(viewModel.uiState.value.hasSystemNotificationPermission).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `setNotificationsEnabled should delegate to repository`() = runTest {
        whenever(notificationRepository.getAllRulesImmediate()).thenReturn(testRules)

        viewModel.onIntent(NotificationIntent.SetEnabled(false))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(notificationsEnabled = true))
        assertThat(updated.notificationsEnabled).isFalse()
        verify(notificationRepository).syncScheduling()
    }

    @Test
    fun `setNotificationsEnabled true with no rules inserts default rule sequentially before updating settings and syncing`() =
        runTest {
            val insertStarted = CompletableDeferred<Unit>()
            val allowInsertToFinish = CompletableDeferred<Unit>()
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(emptyList())
            whenever(notificationRepository.insertRule(any())).doSuspendableAnswer {
                insertStarted.complete(Unit)
                allowInsertToFinish.await()
            }

            viewModel.onIntent(NotificationIntent.SetEnabled(true))
            insertStarted.await()

            // Before insertRule completes, neither updateSettings nor syncScheduling may run
            verify(settingsRepository, never()).updateSettings(any())
            verify(notificationRepository, never()).syncScheduling()

            allowInsertToFinish.complete(Unit)
            runCurrent()

            val ordered = inOrder(notificationRepository, settingsRepository)
            ordered.verify(notificationRepository).insertRule(
                NotificationRule(daysBefore = 0, hour = 9, minute = 0)
            )
            ordered.verify(settingsRepository).updateSettings(any())
            ordered.verify(notificationRepository).syncScheduling()
        }

    @Test
    fun `setPersistentNotifications should delegate to repository`() = runTest {
        viewModel.onIntent(NotificationIntent.SetPersistent(true))

        val captor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(captor.capture())
        val updated = captor.firstValue(AppSettings(persistentNotifications = false))
        assertThat(updated.persistentNotifications).isTrue()
    }

    @Test
    fun `addNotificationRule should delegate to repository`() = runTest {
        viewModel.onIntent(NotificationIntent.AddRule(daysBefore = 2, hour = 10, minute = 30))

        verify(notificationRepository).insertRule(any())
    }

    @Test
    fun `addNotificationRule with duplicate daysBefore should not insert rule into repository`() = runTest {
        // daysBefore = 0 already exists in testRules
        viewModel.onIntent(NotificationIntent.AddRule(daysBefore = 0, hour = 18, minute = 0))

        verify(notificationRepository, never()).insertRule(any())
    }

    @Test
    fun `updateNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.onIntent(NotificationIntent.UpdateRule(rule))
        verify(notificationRepository).updateRule(rule)
    }

    @Test
    fun `replaceRule with changed daysBefore deletes old rule sequentially before inserting new rule`() =
        runTest {
            val oldRule = testRules[0]
            val deleteStarted = CompletableDeferred<Unit>()
            val allowDeleteToFinish = CompletableDeferred<Unit>()

            whenever(notificationRepository.deleteRule(oldRule)).doSuspendableAnswer {
                deleteStarted.complete(Unit)
                allowDeleteToFinish.await()
            }
            whenever(notificationRepository.getAllRulesImmediate()).thenReturn(listOf(testRules[1]))

            viewModel.onIntent(
                NotificationIntent.ReplaceRule(
                    oldRule = oldRule,
                    newDaysBefore = 3,
                    newHour = 10,
                    newMinute = 15
                )
            )
            deleteStarted.await()

            // Insert must not run before delete completes
            verify(notificationRepository, never()).insertRule(any())

            allowDeleteToFinish.complete(Unit)
            runCurrent()

            val ordered = inOrder(notificationRepository)
            ordered.verify(notificationRepository).deleteRule(oldRule)
            ordered.verify(notificationRepository).insertRule(
                NotificationRule(daysBefore = 3, hour = 10, minute = 15)
            )
        }

    @Test
    fun `replaceRule with unchanged daysBefore updates rule instead of deleting`() = runTest {
        val oldRule = testRules[0]

        viewModel.onIntent(
            NotificationIntent.ReplaceRule(
                oldRule = oldRule,
                newDaysBefore = oldRule.daysBefore,
                newHour = 12,
                newMinute = 45
            )
        )

        verify(notificationRepository).updateRule(
            NotificationRule(daysBefore = oldRule.daysBefore, hour = 12, minute = 45)
        )
        verify(notificationRepository, never()).deleteRule(any())
        verify(notificationRepository, never()).insertRule(any())
    }

    @Test
    fun `deleteNotificationRule should delegate to repository`() = runTest {
        val rule = testRules[0]
        viewModel.onIntent(NotificationIntent.DeleteRule(rule))
        verify(notificationRepository).deleteRule(rule)
    }
}
