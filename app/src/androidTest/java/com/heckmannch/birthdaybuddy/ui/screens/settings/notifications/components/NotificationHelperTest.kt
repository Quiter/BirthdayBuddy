package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.notification.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

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

        // Query active notifications with polling to avoid system latency race conditions
        var birthdayNotification: android.service.notification.StatusBarNotification? = null
        for (i in 1..10) {
            val activeNotifications = notificationManager.activeNotifications
            birthdayNotification = activeNotifications.find { it.id == 456 }
            if (birthdayNotification != null) break
            android.util.Log.d("NotificationHelperTest", "Polling active notifications, attempt $i")
            kotlinx.coroutines.delay(100.milliseconds)
        }

        assertThat(birthdayNotification).isNotNull()
        assertThat(birthdayNotification?.notification?.channelId).isEqualTo(NotificationHelper.CHANNEL_ID)

        // Clean up
        notificationManager.cancel(456)
    }
}
