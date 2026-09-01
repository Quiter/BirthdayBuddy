package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHelperTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        "android.permission.POST_NOTIFICATIONS"
    )

    private lateinit var context: Context
    private val notificationRepository: NotificationRepository = mock()
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationManager: NotificationManager

    private val settingsFlow = MutableStateFlow(
        AppSettings(
            notificationsEnabled = true,
            persistentNotifications = false
        )
    )

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clear any active notifications first
        notificationManager.cancelAll()

        whenever(notificationRepository.settings).doReturn(settingsFlow)
        whenever(notificationRepository.getPendingNotificationById(any())).doReturn(null)

        notificationHelper = NotificationHelper(context, notificationRepository)
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
    }

    @Test
    fun showBirthdayNotification_singleContact_postsActiveNotification() = runTest {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 24)
        )

        notificationHelper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 456
        )

        // Query active notifications with real wall-clock polling to prevent system IPC latency race conditions.
        // Thread.sleep is used because runTest virtualizes delay() instantly.
        val birthdayNotification = pollActiveNotification(456)

        assertThat(birthdayNotification).isNotNull()
        assertThat(birthdayNotification?.notification?.channelId).isEqualTo(NotificationHelper.CHANNEL_ID)
    }

    @Test
    fun showBirthdayNotification_multipleContacts_postsActiveNotification() = runTest {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 24)
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            birthday = LocalDate.of(1992, 8, 15)
        )

        notificationHelper.showBirthdayNotification(
            contacts = listOf(contact1, contact2),
            daysBefore = 1,
            pendingId = 789
        )

        val notification = pollActiveNotification(789)

        assertThat(notification).isNotNull()
        assertThat(notification?.notification?.channelId).isEqualTo(NotificationHelper.CHANNEL_ID)
    }

    @Test
    fun showBirthdayNotification_persistentSetting_setsOngoingFlag() = runTest {
        settingsFlow.value = AppSettings(
            notificationsEnabled = true,
            persistentNotifications = true
        )

        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 24)
        )

        notificationHelper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 555
        )

        val notification = pollActiveNotification(555)

        assertThat(notification).isNotNull()
        val isOngoing = (notification?.notification?.flags?.and(android.app.Notification.FLAG_ONGOING_EVENT)) != 0
        assertThat(isOngoing).isTrue()
    }

    @Test
    fun showBirthdayNotification_anniversary_postsActiveNotification() = runTest {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            anniversary = LocalDate.of(2015, 6, 12)
        )

        notificationHelper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 666,
            eventType = EventType.ANNIVERSARY
        )

        val notification = pollActiveNotification(666)

        assertThat(notification).isNotNull()
        assertThat(notification?.notification?.channelId).isEqualTo(NotificationHelper.CHANNEL_ID)
    }

    private fun pollActiveNotification(id: Int, maxAttempts: Int = 20, sleepMs: Long = 50): android.service.notification.StatusBarNotification? {
        for (i in 1..maxAttempts) {
            val activeNotifications = notificationManager.activeNotifications
            val notification = activeNotifications.find { it.id == id }
            if (notification != null) return notification
            try {
                Thread.sleep(sleepMs)
            } catch (_: InterruptedException) {
                // ignore
            }
        }
        return null
    }
}
