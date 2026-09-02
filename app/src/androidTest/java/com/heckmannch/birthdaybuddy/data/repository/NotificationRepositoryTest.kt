package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.AppSettingsMapper
import com.heckmannch.birthdaybuddy.data.mapper.NotificationRuleMapper
import com.heckmannch.birthdaybuddy.data.mapper.PendingNotificationMapper
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var settingsDb: SettingsDatabase
    private lateinit var repository: NotificationRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        settingsDb = Room.inMemoryDatabaseBuilder(context, SettingsDatabase::class.java).build()
        val scheduler = object : NotificationScheduler {
            override fun scheduleNext(rules: List<NotificationRule>) {}
            override fun cancelNotification() {}
            override fun snoozeNotification(
                pendingId: Int,
                daysBefore: Int,
                lookupKeys: List<String>
            ) {
            }

            override fun reshowNotification(
                pendingId: Int,
                daysBefore: Int,
                lookupKeys: List<String>,
                eventType: EventType,
                delayMillis: Long
            ) {
            }
        }
        repository = NotificationRepositoryImpl(
            notificationRuleDao = settingsDb.notificationRuleDao(),
            pendingNotificationDao = db.pendingNotificationDao(),
            appSettingsDao = settingsDb.appSettingsDao(),
            notificationScheduler = scheduler,
            appSettingsMapper = AppSettingsMapper(),
            notificationRuleMapper = NotificationRuleMapper(),
            pendingNotificationMapper = PendingNotificationMapper(),
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO,
            defaultDispatcher = kotlinx.coroutines.Dispatchers.Default
        )
    }

    @After
    fun closeDb() {
        db.close()
        settingsDb.close()
    }

    @Test
    fun updateSettings_consecutiveUpdates_areConsistent() = runTest {
        // Initial state
        repository.updateSettings {
            it.copy(notificationsEnabled = false, onboardingCompleted = false)
        }

        // Simulating the race condition: Two updates fired "at the same time"
        // Without Mutex, one could read the old state before the other writes,
        // resulting in one setting being lost.
        val job1 = async { repository.updateSettings { it.copy(notificationsEnabled = true) } }
        val job2 = async { repository.updateSettings { it.copy(onboardingCompleted = true) } }

        job1.await()
        job2.await()

        val finalSettings = repository.settings.first()

        // Both should be true
        assertThat(finalSettings.notificationsEnabled).isTrue()
        assertThat(finalSettings.onboardingCompleted).isTrue()
    }

    @Test
    fun updateSettings_partialUpdates_doNotOverwriteOthers() = runTest {
        // Set something first
        repository.updateSettings { it.copy(persistentNotifications = false) }

        // Update something else
        repository.updateSettings { it.copy(notificationsEnabled = true) }

        val finalSettings = repository.settings.first()

        // Check both
        assertThat(finalSettings.persistentNotifications).isFalse()
        assertThat(finalSettings.notificationsEnabled).isTrue()
    }
}
