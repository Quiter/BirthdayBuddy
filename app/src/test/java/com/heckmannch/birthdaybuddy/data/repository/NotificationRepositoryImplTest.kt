package com.heckmannch.birthdaybuddy.data.repository

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleDao
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleEntity
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationDao
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationEntity
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.data.mapper.NotificationRuleMapper
import com.heckmannch.birthdaybuddy.data.mapper.PendingNotificationMapper
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * JVM Unit Tests for [NotificationRepositoryImpl].
 *
 * This test suite secures the logic for application settings, notification scheduling rules,
 * and pending notification management, meeting Project Guideline §2.6 test coverage requirements.
 *
 * It uses MockK for mocking DAO layer dependencies and [NotificationScheduler], and leverages
 * the [MainDispatcherRule] to ensure coroutines run synchronously on the UnconfinedTestDispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies with mockk(relaxed = true)
    private val notificationRuleDao: NotificationRuleDao = mockk(relaxed = true)
    private val pendingNotificationDao: PendingNotificationDao = mockk(relaxed = true)
    private val appSettingsDao: AppSettingsDao = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    // Concrete mapper dependencies
    private val appSettingsMapper = AppSettingsMapper()
    private val notificationRuleMapper = NotificationRuleMapper()
    private val pendingNotificationMapper = PendingNotificationMapper()

    // State flows to back DAO mocks
    private val allRulesFlow = MutableStateFlow<List<NotificationRuleEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow<AppSettingsEntity?>(null)

    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        // Stub flows accessed during initialization
        every { notificationRuleDao.getAllRules() } returns allRulesFlow
        every { appSettingsDao.getSettings() } returns settingsFlow

        repository = NotificationRepositoryImpl(
            notificationRuleDao = notificationRuleDao,
            pendingNotificationDao = pendingNotificationDao,
            appSettingsDao = appSettingsDao,
            notificationScheduler = notificationScheduler,
            appSettingsMapper = appSettingsMapper,
            notificationRuleMapper = notificationRuleMapper,
            pendingNotificationMapper = pendingNotificationMapper
        )
    }

    @Test
    fun allRules_emitsCorrectlyMappedDomainObjects() = runTest {
        // Arrange
        val ruleEntities = listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0),
            NotificationRuleEntity(id = 2, daysBefore = 1, hour = 18, minute = 30)
        )
        allRulesFlow.value = ruleEntities

        // Act
        val result = repository.allRules.first()

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[0].daysBefore).isEqualTo(0)
        assertThat(result[0].hour).isEqualTo(9)
        assertThat(result[0].minute).isEqualTo(0)

        assertThat(result[1].id).isEqualTo(2)
        assertThat(result[1].daysBefore).isEqualTo(1)
        assertThat(result[1].hour).isEqualTo(18)
        assertThat(result[1].minute).isEqualTo(30)
    }

    @Test
    fun settings_emitsCorrectlyMappedDomainObjects() = runTest {
        // Arrange
        val settingsEntity = AppSettingsEntity(
            id = 0,
            notificationsEnabled = true,
            persistentNotifications = false,
            onboardingCompleted = true,
            themeMode = ThemeMode.DARK
        )
        settingsFlow.value = settingsEntity

        // Act
        val result = repository.settings.first()

        // Assert
        assertThat(result.id).isEqualTo(0)
        assertThat(result.notificationsEnabled).isTrue()
        assertThat(result.persistentNotifications).isFalse()
        assertThat(result.onboardingCompleted).isTrue()
        assertThat(result.themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun settings_emitsDefaultAppSettings_whenEntityIsNull() = runTest {
        // Arrange
        settingsFlow.value = null

        // Act
        val result = repository.settings.first()

        // Assert
        assertThat(result).isEqualTo(AppSettings())
    }

    @Test
    fun syncScheduling_schedulesNext_whenEnabledAndRulesNotEmpty() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = true)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0)
        )

        // Act
        repository.syncScheduling()

        // Assert
        val capturedRules = slot<List<NotificationRule>>()
        coVerify { notificationScheduler.scheduleNext(capture(capturedRules)) }
        assertThat(capturedRules.captured).hasSize(1)
        assertThat(capturedRules.captured[0].id).isEqualTo(1)
        coVerify(exactly = 0) { notificationScheduler.cancelNotification() }
    }

    @Test
    fun syncScheduling_cancelsNotification_whenDisabled() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = false)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0)
        )

        // Act
        repository.syncScheduling()

        // Assert
        coVerify { notificationScheduler.cancelNotification() }
        coVerify(exactly = 0) { notificationScheduler.scheduleNext(any()) }
    }

    @Test
    fun syncScheduling_cancelsNotification_whenRulesEmpty() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = true)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.syncScheduling()

        // Assert
        coVerify { notificationScheduler.cancelNotification() }
        coVerify(exactly = 0) { notificationScheduler.scheduleNext(any()) }
    }

    @Test
    fun syncScheduling_cancelsNotification_whenSettingsNull() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns null
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0)
        )

        // Act
        repository.syncScheduling()

        // Assert
        coVerify { notificationScheduler.cancelNotification() }
        coVerify(exactly = 0) { notificationScheduler.scheduleNext(any()) }
    }

    @Test
    fun updateSettings_upsertsCorrectSettingsAndTriggersSync() = runTest {
        // Arrange
        val currentSettings = AppSettingsEntity(
            id = 0,
            notificationsEnabled = false,
            persistentNotifications = true,
            themeMode = ThemeMode.LIGHT
        )
        coEvery { appSettingsDao.getSettingsImmediate() } returns currentSettings
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.updateSettings(
            notificationsEnabled = true,
            persistentNotifications = null,
            onboardingCompleted = null,
            lastSyncTimestamp = null,
            calendarSyncEnabled = null,
            calendarId = 42L,
            clearCalendarId = false,
            otherEventsEnabled = null,
            birthdayCalendarColor = null,
            anniversaryCalendarColor = null,
            nameDayCalendarColor = null,
            themeMode = ThemeMode.DARK,
            themeAmoled = null,
            themeAccent = null
        )

        // Assert
        val capturedEntity = slot<AppSettingsEntity>()
        coVerify { appSettingsDao.upsertSettings(capture(capturedEntity)) }
        val saved = capturedEntity.captured
        assertThat(saved.notificationsEnabled).isTrue()
        assertThat(saved.persistentNotifications).isTrue()
        assertThat(saved.calendarId).isEqualTo(42L)
        assertThat(saved.themeMode).isEqualTo(ThemeMode.DARK)

        // Verify sync scheduling is triggered
        coVerify { appSettingsDao.getSettingsImmediate() }
        coVerify { notificationScheduler.cancelNotification() }
    }

    @Test
    fun updateSettings_clearsCalendarId_whenClearCalendarIdIsTrue() = runTest {
        // Arrange
        val currentSettings = AppSettingsEntity(
            id = 0,
            calendarId = 42L
        )
        coEvery { appSettingsDao.getSettingsImmediate() } returns currentSettings
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.updateSettings(
            calendarId = null,
            clearCalendarId = true
        )

        // Assert
        val capturedEntity = slot<AppSettingsEntity>()
        coVerify { appSettingsDao.upsertSettings(capture(capturedEntity)) }
        assertThat(capturedEntity.captured.calendarId).isNull()
    }

    @Test
    fun getAllRulesImmediate_returnsMappedRules() = runTest {
        // Arrange
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0)
        )

        // Act
        val result = repository.getAllRulesImmediate()

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1)
    }

    @Test
    fun insertRule_delegatesToDaoAndTriggersSync() = runTest {
        // Arrange
        val rule = NotificationRule(id = 5, daysBefore = 2, hour = 12, minute = 0)
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = false)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.insertRule(rule)

        // Assert
        val capturedRuleEntity = slot<NotificationRuleEntity>()
        coVerify { notificationRuleDao.upsertRule(capture(capturedRuleEntity)) }
        assertThat(capturedRuleEntity.captured.id).isEqualTo(5)
        assertThat(capturedRuleEntity.captured.daysBefore).isEqualTo(2)

        // Verify sync scheduling is triggered
        coVerify { notificationScheduler.cancelNotification() }
    }

    @Test
    fun updateRule_delegatesToDaoAndTriggersSync() = runTest {
        // Arrange
        val rule = NotificationRule(id = 5, daysBefore = 2, hour = 12, minute = 0)
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = false)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.updateRule(rule)

        // Assert
        val capturedRuleEntity = slot<NotificationRuleEntity>()
        coVerify { notificationRuleDao.updateRule(capture(capturedRuleEntity)) }
        assertThat(capturedRuleEntity.captured.id).isEqualTo(5)

        // Verify sync scheduling is triggered
        coVerify { notificationScheduler.cancelNotification() }
    }

    @Test
    fun deleteRule_delegatesToDaoAndTriggersSync() = runTest {
        // Arrange
        val rule = NotificationRule(id = 5, daysBefore = 2, hour = 12, minute = 0)
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(notificationsEnabled = false)
        coEvery { notificationRuleDao.getAllRulesImmediate() } returns emptyList()

        // Act
        repository.deleteRule(rule)

        // Assert
        val capturedRuleEntity = slot<NotificationRuleEntity>()
        coVerify { notificationRuleDao.deleteRule(capture(capturedRuleEntity)) }
        assertThat(capturedRuleEntity.captured.id).isEqualTo(5)

        // Verify sync scheduling is triggered
        coVerify { notificationScheduler.cancelNotification() }
    }

    @Test
    fun getActiveNotificationsImmediate_returnsMappedNotifications() = runTest {
        // Arrange
        coEvery { pendingNotificationDao.getActiveNotificationsImmediate() } returns listOf(
            PendingNotificationEntity(id = 1, contactLookupKeys = listOf("key1"), daysBefore = 0, year = 2024, isDone = false, dismissCount = 0)
        )

        // Act
        val result = repository.getActiveNotificationsImmediate()

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[0].contactLookupKeys).containsExactly("key1")
        assertThat(result[0].daysBefore).isEqualTo(0)
        assertThat(result[0].year).isEqualTo(2024)
        assertThat(result[0].isDone).isFalse()
        assertThat(result[0].dismissCount).isEqualTo(0)
    }

    @Test
    fun insertPendingNotification_delegatesToDaoAndReturnsId() = runTest {
        // Arrange
        val notification = PendingNotification(id = 0, contactLookupKeys = listOf("key1"), daysBefore = 1, year = 2024, isDone = false, dismissCount = 2)
        coEvery { pendingNotificationDao.upsert(any()) } returns 101L

        // Act
        val result = repository.insertPendingNotification(notification)

        // Assert
        assertThat(result).isEqualTo(101L)
        val capturedEntity = slot<PendingNotificationEntity>()
        coVerify { pendingNotificationDao.upsert(capture(capturedEntity)) }
        assertThat(capturedEntity.captured.id).isEqualTo(0)
        assertThat(capturedEntity.captured.contactLookupKeys).containsExactly("key1")
        assertThat(capturedEntity.captured.daysBefore).isEqualTo(1)
        assertThat(capturedEntity.captured.year).isEqualTo(2024)
        assertThat(capturedEntity.captured.isDone).isFalse()
        assertThat(capturedEntity.captured.dismissCount).isEqualTo(2)
    }

    @Test
    fun getPendingNotificationById_returnsMappedNotification_whenExists() = runTest {
        // Arrange
        coEvery { pendingNotificationDao.getNotificationById(123) } returns PendingNotificationEntity(
            id = 123, contactLookupKeys = listOf("key1"), daysBefore = 0, year = 2024, isDone = true, dismissCount = 1
        )

        // Act
        val result = repository.getPendingNotificationById(123)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(123)
        assertThat(result.isDone).isTrue()
        assertThat(result.dismissCount).isEqualTo(1)
    }

    @Test
    fun getPendingNotificationById_returnsNull_whenDoesNotExist() = runTest {
        // Arrange
        coEvery { pendingNotificationDao.getNotificationById(123) } returns null

        // Act
        val result = repository.getPendingNotificationById(123)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun hasNotificationBeenScheduled_delegatesToDaoWithWildcardPattern() = runTest {
        // Arrange
        coEvery { pendingNotificationDao.hasNotificationBeenScheduled(2024, 1, "%\"key_abc\"%") } returns true

        // Act
        val result = repository.hasNotificationBeenScheduled(2024, 1, "key_abc")

        // Assert
        assertThat(result).isTrue()
        coVerify { pendingNotificationDao.hasNotificationBeenScheduled(2024, 1, "%\"key_abc\"%") }
    }

    @Test
    fun incrementDismissCount_delegatesToDao() = runTest {
        // Act
        repository.incrementDismissCount(123)

        // Assert
        coVerify { pendingNotificationDao.incrementDismissCount(123) }
    }

    @Test
    fun markAsDone_delegatesToDao() = runTest {
        // Act
        repository.markAsDone(123)

        // Assert
        coVerify { pendingNotificationDao.markAsDone(123) }
    }

    @Test
    fun deleteOldNotifications_delegatesToDao() = runTest {
        // Act
        repository.deleteOldNotifications(2024)

        // Assert
        coVerify { pendingNotificationDao.deleteOldNotifications(2024) }
    }
}
