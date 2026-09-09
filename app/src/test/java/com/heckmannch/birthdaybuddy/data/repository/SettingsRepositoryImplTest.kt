package com.heckmannch.birthdaybuddy.data.repository

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * JVM Unit Tests for [SettingsRepositoryImpl].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appSettingsDao: AppSettingsDao = mockk(relaxed = true)
    private val appSettingsMapper = AppSettingsMapper()
    private val settingsFlow = MutableStateFlow<AppSettingsEntity?>(null)

    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        every { appSettingsDao.getSettings() } returns settingsFlow
        repository = SettingsRepositoryImpl(
            appSettingsDao = appSettingsDao,
            appSettingsMapper = appSettingsMapper,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun settings_emitsCorrectlyMappedDomainObjects() = runTest {
        // Arrange
        val entity = AppSettingsEntity(
            id = 0,
            notificationsEnabled = true,
            persistentNotifications = false,
            onboardingCompleted = true,
            themeModeString = "DARK"
        )
        settingsFlow.value = entity

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
    fun updateSettings_upsertsCorrectSettings() = runTest {
        // Arrange
        val currentEntity = AppSettingsEntity(
            id = 0,
            notificationsEnabled = false,
            persistentNotifications = true,
            themeModeString = "LIGHT"
        )
        coEvery { appSettingsDao.getSettingsImmediate() } returns currentEntity

        // Act
        repository.updateSettings {
            it.copy(
                notificationsEnabled = true,
                calendarId = 42L,
                themeMode = ThemeMode.DARK
            )
        }

        // Assert
        val slot = slot<AppSettingsEntity>()
        coVerify { appSettingsDao.upsertSettings(capture(slot)) }
        val saved = slot.captured
        assertThat(saved.notificationsEnabled).isTrue()
        assertThat(saved.persistentNotifications).isTrue()
        assertThat(saved.calendarId).isEqualTo(42L)
        assertThat(saved.themeModeString).isEqualTo("DARK")
    }

    @Test
    fun updateSettings_whenCurrentNull_usesDefaultsAndUpserts() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns null

        // Act
        repository.updateSettings {
            it.copy(labelsEnabled = false)
        }

        // Assert
        val slot = slot<AppSettingsEntity>()
        coVerify { appSettingsDao.upsertSettings(capture(slot)) }
        assertThat(slot.captured.labelsEnabled).isFalse()
    }

    @Test
    fun updateSettings_concurrentCalls_executeSequentiallyWithoutRaceCondition() = runTest {
        // Arrange
        val entity = AppSettingsEntity(id = 0)
        coEvery { appSettingsDao.getSettingsImmediate() } returns entity

        val executingCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        coEvery { appSettingsDao.upsertSettings(any()) } coAnswers {
            val current = executingCount.incrementAndGet()
            maxConcurrent.updateAndGet { maxOf(it, current) }
            delay(50.milliseconds)
            executingCount.decrementAndGet()
        }

        // Act
        val job1 = launch { repository.updateSettings { it.copy(notificationsEnabled = true) } }
        val job2 = launch { repository.updateSettings { it.copy(notificationsEnabled = false) } }

        job1.join()
        job2.join()

        // Assert
        assertThat(maxConcurrent.get()).isEqualTo(1)
        coVerify(exactly = 2) { appSettingsDao.upsertSettings(any()) }
    }

    @Test
    fun getSettingsImmediate_returnsMappedSettings() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns AppSettingsEntity(
            notificationsEnabled = true,
            persistentNotifications = false
        )

        // Act
        val result = repository.getSettingsImmediate()

        // Assert
        assertThat(result.notificationsEnabled).isTrue()
        assertThat(result.persistentNotifications).isFalse()
    }

    @Test
    fun getSettingsImmediate_whenDaoReturnsNull_returnsDefaultSettings() = runTest {
        // Arrange
        coEvery { appSettingsDao.getSettingsImmediate() } returns null

        // Act
        val result = repository.getSettingsImmediate()

        // Assert
        assertThat(result).isEqualTo(AppSettings())
    }
}
