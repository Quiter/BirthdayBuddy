package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
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
    private lateinit var repository: NotificationRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = NotificationRepository(
            notificationRuleDao = db.notificationRuleDao(),
            pendingNotificationDao = db.pendingNotificationDao(),
            appSettingsDao = db.appSettingsDao()
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun updateSettings_consecutiveUpdates_areConsistent() = runTest {
        // Initial state
        repository.updateSettings(notificationsEnabled = false, onboardingCompleted = false)

        // Simulating the race condition: Two updates fired "at the same time"
        // Without Mutex, one could read the old state before the other writes,
        // resulting in one setting being lost.
        val job1 = async { repository.updateSettings(notificationsEnabled = true) }
        val job2 = async { repository.updateSettings(onboardingCompleted = true) }

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
        repository.updateSettings(persistentNotifications = false)

        // Update something else
        repository.updateSettings(notificationsEnabled = true)

        val finalSettings = repository.settings.first()

        // Check both
        assertThat(finalSettings.persistentNotifications).isFalse()
        assertThat(finalSettings.notificationsEnabled).isTrue()
    }
}
