package com.heckmannch.birthdaybuddy.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainActivity
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.util.IntentExtras
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Unit tests for [NotificationHelper] verifying:
 * - Notification channel creation with correct ID, name, description, and importance.
 * - Notification building including titles, content text, flags (ongoing, autoCancel),
 *   and PendingIntent actions (snooze, done, settings, dismiss).
 * - Suppression of notifications when notifications are disabled.
 * - Calculation of notification IDs using either the database ID or fallback calculation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationHelperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var baseContext: Context
    private lateinit var context: Context
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val notificationTextFormatter = mockk<NotificationTextFormatter>(relaxed = true)
    private val notificationManagerCompat = mockk<NotificationManagerCompat>()

    private val settingsFlow = MutableStateFlow(
        AppSettings(
            notificationsEnabled = true,
            persistentNotifications = false
        )
    )

    private lateinit var helper: NotificationHelper

    @Before
    fun setUp() {
        baseContext = ApplicationProvider.getApplicationContext()
        context = spyk(baseContext) {
            every { getSystemService(NotificationManager::class.java) } returns notificationManager
            every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        }

        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManagerCompat
        every { notificationManagerCompat.areNotificationsEnabled() } returns true

        every { settingsRepository.settings } returns settingsFlow
        coEvery { notificationRepository.getPendingNotificationById(any()) } returns null

        every {
            notificationTextFormatter.buildTitle(any(), any(), any(), any())
        } returns "Test Birthday Title"
        every {
            notificationTextFormatter.buildContentText(any(), any(), any())
        } returns "Test Birthday Content"

        helper = NotificationHelper(context, notificationRepository, settingsRepository, notificationTextFormatter)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // 1. Notification Channel Creation Tests
    // =========================================================================

    @Test
    fun showBirthdayNotification_createsNotificationChannelWithCorrectConfiguration() = runTest {
        val contact = Contact(contactId = "1", lookupKey = "lookup1", fullName = "Max Mustermann")
        val channelSlot = slot<NotificationChannel>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 101
        )

        verify(exactly = 1) { notificationManager.createNotificationChannel(capture(channelSlot)) }
        val channel = channelSlot.captured

        assertThat(channel.id).isEqualTo(NotificationHelper.CHANNEL_ID)
        assertThat(channel.name).isEqualTo(context.getString(R.string.notif_channel_name))
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(channel.description).isEqualTo(context.getString(R.string.notif_channel_desc))
    }

    // =========================================================================
    // 2. Notification Building Tests (Title, Text, PendingIntent Actions, Flags)
    // =========================================================================

    @Suppress("DEPRECATION")
    @Test
    fun showBirthdayNotification_nonPersistent_buildsNotificationWithSnoozeActionAndAutoCancel() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = false)

        val contact = Contact(
            contactId = "1",
            lookupKey = "key_max",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 20)
        )
        val notificationSlot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 1,
            pendingId = 501,
            eventType = EventType.BIRTHDAY
        )

        verify(exactly = 1) { notificationManager.notify(501, capture(notificationSlot)) }
        val notification = notificationSlot.captured

        // Channel and Priority
        assertThat(notification.channelId).isEqualTo(NotificationHelper.CHANNEL_ID)
        assertThat(notification.priority).isEqualTo(NotificationCompat.PRIORITY_HIGH)

        // Title and Text from Formatter
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            .isEqualTo("Test Birthday Title")
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            .isEqualTo("Test Birthday Content")

        // Flags: Non-persistent must NOT be ongoing and MUST have AUTO_CANCEL
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isAutoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        assertThat(isOngoing).isFalse()
        assertThat(isAutoCancel).isTrue()

        // Content Intent points to MainActivity
        assertThat(notification.contentIntent).isNotNull()
        val contentShadow = shadowOf(notification.contentIntent)
        assertThat(contentShadow.savedIntent.component?.className).isEqualTo(MainActivity::class.java.name)

        // Non-persistent has NO deleteIntent and only SNOOZE action
        assertThat(notification.deleteIntent).isNull()
        assertThat(notification.actions).hasLength(1)

        val snoozeAction = notification.actions[0]
        assertThat(snoozeAction.title.toString()).isEqualTo(context.getString(R.string.notif_action_snooze))

        val snoozeShadow = shadowOf(snoozeAction.actionIntent)
        val snoozeIntent = snoozeShadow.savedIntent
        assertThat(snoozeIntent.action).isEqualTo(NotificationActions.ACTION_SNOOZE)
        assertThat(snoozeIntent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1)).isEqualTo(501)
        assertThat(snoozeIntent.getIntExtra(NotificationActions.EXTRA_PENDING_ID, -1)).isEqualTo(501)
        assertThat(snoozeIntent.getIntExtra(NotificationActions.EXTRA_DAYS_BEFORE, -1)).isEqualTo(1)
        assertThat(snoozeIntent.getStringArrayExtra(NotificationActions.EXTRA_LOOKUP_KEYS)?.toList())
            .containsExactly("key_max")
    }

    @Test
    fun showBirthdayNotification_persistentWithoutHint_addsDoneActionAndOngoingFlag() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        coEvery { notificationRepository.getPendingNotificationById(502) } returns PendingNotification(
            contactLookupKeys = listOf("key_anna"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 1 // Below hint threshold of 3
        )

        val contact = Contact(contactId = "2", lookupKey = "key_anna", fullName = "Anna Schmidt")
        val notificationSlot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 502,
            eventType = EventType.BIRTHDAY
        )

        verify(exactly = 1) { notificationManager.notify(502, capture(notificationSlot)) }
        val notification = notificationSlot.captured

        // Flags: Persistent must be ongoing and not auto-cancel
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isAutoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0
        assertThat(isOngoing).isTrue()
        assertThat(isAutoCancel).isFalse()

        // DeleteIntent must be set for persistent dismissal handling
        assertThat(notification.deleteIntent).isNotNull()
        val deleteShadow = shadowOf(notification.deleteIntent)
        assertThat(deleteShadow.savedIntent.action).isEqualTo(NotificationActions.ACTION_DISMISSED)
        assertThat(deleteShadow.savedIntent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1))
            .isEqualTo(502)
        assertThat(deleteShadow.savedIntent.getIntExtra(NotificationActions.EXTRA_PENDING_ID, -1))
            .isEqualTo(502)

        // Persistent without hint has 2 actions: DONE and SNOOZE
        assertThat(notification.actions).hasLength(2)
        val actionTitles = notification.actions.map { it.title.toString() }
        assertThat(actionTitles).containsExactly(
            context.getString(R.string.notif_action_done),
            context.getString(R.string.notif_action_snooze)
        ).inOrder()

        val doneAction = notification.actions[0]
        val doneShadow = shadowOf(doneAction.actionIntent)
        assertThat(doneShadow.savedIntent.action).isEqualTo(NotificationActions.ACTION_DONE)
        assertThat(doneShadow.savedIntent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1))
            .isEqualTo(502)
        assertThat(doneShadow.savedIntent.getIntExtra(NotificationActions.EXTRA_PENDING_ID, -1))
            .isEqualTo(502)
    }

    @Test
    fun showBirthdayNotification_persistentWithHighDismissCount_addsSettingsActionAndPassesShowHint() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        coEvery { notificationRepository.getPendingNotificationById(503) } returns PendingNotification(
            contactLookupKeys = listOf("key_tom"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 3 // Threshold for showing persistent hint
        )

        val contact = Contact(contactId = "3", lookupKey = "key_tom", fullName = "Tom Becker")
        val notificationSlot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 503,
            eventType = EventType.BIRTHDAY
        )

        verify(exactly = 1) { notificationManager.notify(503, capture(notificationSlot)) }
        val notification = notificationSlot.captured

        // Verify that formatter was invoked with showHint = true
        verify {
            notificationTextFormatter.buildContentText(
                contacts = listOf(contact),
                eventType = EventType.BIRTHDAY,
                showHint = true
            )
        }

        // Persistent with hint has 3 actions: DONE, SETTINGS, SNOOZE
        assertThat(notification.actions).hasLength(3)
        val actionTitles = notification.actions.map { it.title.toString() }
        assertThat(actionTitles).containsExactly(
            context.getString(R.string.notif_action_done),
            context.getString(R.string.notif_action_settings),
            context.getString(R.string.notif_action_snooze)
        ).inOrder()

        val settingsAction = notification.actions[1]
        val settingsShadow = shadowOf(settingsAction.actionIntent)
        assertThat(settingsShadow.savedIntent.component?.className).isEqualTo(MainActivity::class.java.name)
        assertThat(
            settingsShadow.savedIntent.getBooleanExtra(
                IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
                false
            )
        ).isTrue()
    }

    @Test
    fun showBirthdayNotification_encodesEventTypeInLookupKeys() = runTest {
        val contact = Contact(contactId = "4", lookupKey = "key_anniv", fullName = "Ehepaar")
        val notificationSlot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 504,
            eventType = EventType.ANNIVERSARY
        )

        verify(exactly = 1) { notificationManager.notify(504, capture(notificationSlot)) }
        val notification = notificationSlot.captured

        val snoozeAction = notification.actions[0]
        val snoozeIntent = shadowOf(snoozeAction.actionIntent).savedIntent
        val lookupKeys = snoozeIntent.getStringArrayExtra(NotificationActions.EXTRA_LOOKUP_KEYS)
        assertThat(lookupKeys?.toList()).containsExactly("anniversary:key_anniv")
    }

    // =========================================================================
    // 3. Permission Checks Tests (areNotificationsEnabled)
    // =========================================================================

    @Test
    fun showBirthdayNotification_whenNotificationsAreDisabled_doesNotPostNotification() = runTest {
        every { notificationManagerCompat.areNotificationsEnabled() } returns false

        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 601
        )

        // Channel is still created, but notify is skipped
        verify(exactly = 1) { notificationManager.createNotificationChannel(any()) }
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }

    @Test
    fun showBirthdayNotification_whenNotificationsAreEnabled_postsNotification() = runTest {
        every { notificationManagerCompat.areNotificationsEnabled() } returns true

        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 602
        )

        verify(exactly = 1) { notificationManager.notify(602, any()) }
    }

    // =========================================================================
    // 4. Notification ID Calculation Tests
    // =========================================================================

    @Test
    fun showBirthdayNotification_withExplicitPendingId_usesPendingIdAsNotificationId() = runTest {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 3,
            pendingId = 777
        )

        verify(exactly = 1) { notificationManager.notify(777, any()) }
    }

    @Test
    fun showBirthdayNotification_withoutPendingIdAndDaysBefore0_usesFallbackNotificationIdBase() = runTest {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = -1
        )

        val expectedId = NotificationHelper.FALLBACK_NOTIFICATION_ID_BASE + 0 // 200
        verify(exactly = 1) { notificationManager.notify(expectedId, any()) }
    }

    @Test
    fun showBirthdayNotification_withoutPendingIdAndDaysBefore7_addsDaysBeforeToFallbackBase() = runTest {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 7,
            pendingId = -1
        )

        val expectedId = NotificationHelper.FALLBACK_NOTIFICATION_ID_BASE + 7 // 207
        verify(exactly = 1) { notificationManager.notify(expectedId, any()) }
    }

    @Test
    fun showBirthdayNotification_whenNotificationManagerIsNull_returnsGracefully() = runTest {
        every { context.getSystemService(NotificationManager::class.java) } returns null
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns null

        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max")

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 999
        )

        verify(exactly = 0) { notificationManager.createNotificationChannel(any()) }
        verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }

    // =========================================================================
    // 5. PendingIntent Request Code Disjointness & Collision Prevention Tests
    // =========================================================================

    @Test
    fun showBirthdayNotification_assignsModulo10RequestCodesForAllPendingIntents() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        coEvery { notificationRepository.getPendingNotificationById(5) } returns PendingNotification(
            contactLookupKeys = listOf("key_5"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 3
        )

        val contact = Contact(contactId = "5", lookupKey = "key_5", fullName = "Lisa")
        val notificationSlot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(contact),
            daysBefore = 0,
            pendingId = 5
        )

        verify(exactly = 1) { notificationManager.notify(5, capture(notificationSlot)) }
        val notification = notificationSlot.captured

        assertThat(shadowOf(notification.contentIntent).requestCode)
            .isEqualTo(5 * 10 + NotificationHelper.OFFSET_CONTENT) // 50
        assertThat(shadowOf(notification.actions[0].actionIntent).requestCode)
            .isEqualTo(5 * 10 + NotificationHelper.OFFSET_DONE) // 52
        assertThat(shadowOf(notification.actions[1].actionIntent).requestCode)
            .isEqualTo(5 * 10 + NotificationHelper.OFFSET_SETTINGS) // 54
        assertThat(shadowOf(notification.actions[2].actionIntent).requestCode)
            .isEqualTo(5 * 10 + NotificationHelper.OFFSET_SNOOZE) // 51
        assertThat(shadowOf(notification.deleteIntent).requestCode)
            .isEqualTo(5 * 10 + NotificationHelper.OFFSET_DISMISSED) // 53
    }

    @Test
    fun showBirthdayNotification_pendingId1AndPendingId14_doNotCollideOrOverwriteExtras() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        coEvery { notificationRepository.getPendingNotificationById(1) } returns PendingNotification(
            contactLookupKeys = listOf("key_1"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 3
        )
        coEvery { notificationRepository.getPendingNotificationById(14) } returns PendingNotification(
            contactLookupKeys = listOf("key_14"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 3
        )

        val notif1Slot = slot<Notification>()
        val notif14Slot = slot<Notification>()

        helper.showBirthdayNotification(
            contacts = listOf(Contact(contactId = "1", lookupKey = "key_1", fullName = "Person 1")),
            daysBefore = 0,
            pendingId = 1
        )
        verify(exactly = 1) { notificationManager.notify(1, capture(notif1Slot)) }

        helper.showBirthdayNotification(
            contacts = listOf(Contact(contactId = "14", lookupKey = "key_14", fullName = "Person 14")),
            daysBefore = 0,
            pendingId = 14
        )
        verify(exactly = 1) { notificationManager.notify(14, capture(notif14Slot)) }

        val notif1 = notif1Slot.captured
        val notif14 = notif14Slot.captured

        val notif1ContentShadow = shadowOf(notif1.contentIntent)
        val notif1SettingsShadow = shadowOf(notif1.actions[1].actionIntent)
        val notif14ContentShadow = shadowOf(notif14.contentIntent)
        val notif14SettingsShadow = shadowOf(notif14.actions[1].actionIntent)

        assertThat(notif1ContentShadow.requestCode).isEqualTo(10)
        assertThat(notif1SettingsShadow.requestCode).isEqualTo(14)
        assertThat(notif14ContentShadow.requestCode).isEqualTo(140)
        assertThat(notif14SettingsShadow.requestCode).isEqualTo(144)

        // Ensure FLAG_UPDATE_CURRENT on notification 14 did not overwrite notification 1's settings extras
        assertThat(
            notif1SettingsShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isTrue()
        assertThat(
            notif1ContentShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isFalse()
        assertThat(
            notif14ContentShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isFalse()
        assertThat(
            notif14SettingsShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isTrue()
    }

    @Test
    fun showBirthdayNotification_fallbackNotificationId_usesModulo10SchemaWithoutCollidingWithPendingId20() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        coEvery { notificationRepository.getPendingNotificationById(20) } returns PendingNotification(
            contactLookupKeys = listOf("key_20"),
            daysBefore = 0,
            year = 2026,
            dismissCount = 3
        )

        val notif20Slot = slot<Notification>()
        val fallbackSlot = slot<Notification>()

        // pendingId = 20 -> settings requestCode = 204
        helper.showBirthdayNotification(
            contacts = listOf(Contact(contactId = "20", lookupKey = "key_20", fullName = "Person 20")),
            daysBefore = 0,
            pendingId = 20
        )
        verify(exactly = 1) { notificationManager.notify(20, capture(notif20Slot)) }

        // Fallback with daysBefore = 4 -> notificationId = 204 -> content requestCode = 2040
        helper.showBirthdayNotification(
            contacts = listOf(Contact(contactId = "21", lookupKey = "key_21", fullName = "Fallback Person")),
            daysBefore = 4,
            pendingId = -1
        )
        val expectedFallbackId = NotificationHelper.FALLBACK_NOTIFICATION_ID_BASE + 4
        verify(exactly = 1) { notificationManager.notify(expectedFallbackId, capture(fallbackSlot)) }

        val notif20SettingsShadow = shadowOf(notif20Slot.captured.actions[1].actionIntent)
        val fallbackContentShadow = shadowOf(fallbackSlot.captured.contentIntent)
        val fallbackDoneShadow = shadowOf(fallbackSlot.captured.actions[0].actionIntent)
        val fallbackSnoozeShadow = shadowOf(fallbackSlot.captured.actions[1].actionIntent)
        val fallbackDeleteShadow = shadowOf(fallbackSlot.captured.deleteIntent)

        assertThat(notif20SettingsShadow.requestCode).isEqualTo(204)
        assertThat(fallbackContentShadow.requestCode).isEqualTo(2040)
        assertThat(fallbackSnoozeShadow.requestCode).isEqualTo(2041)
        assertThat(fallbackDoneShadow.requestCode).isEqualTo(2042)
        assertThat(fallbackDeleteShadow.requestCode).isEqualTo(2043)

        assertThat(
            notif20SettingsShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isTrue()
        assertThat(
            fallbackContentShadow.savedIntent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false)
        ).isFalse()
    }

    @Test
    fun showBirthdayNotification_requestCodesArePairwiseDisjointAcrossPendingIds() = runTest {
        settingsFlow.value = AppSettings(notificationsEnabled = true, persistentNotifications = true)
        val seenRequestCodes = mutableSetOf<Int>()
        val contact = Contact(contactId = "1", lookupKey = "key", fullName = "Test")

        for (pendingId in 1..25) {
            coEvery { notificationRepository.getPendingNotificationById(pendingId) } returns PendingNotification(
                contactLookupKeys = listOf("key"),
                daysBefore = 0,
                year = 2026,
                dismissCount = 3
            )
            val notificationSlot = slot<Notification>()

            helper.showBirthdayNotification(
                contacts = listOf(contact),
                daysBefore = 0,
                pendingId = pendingId
            )

            verify(exactly = 1) { notificationManager.notify(pendingId, capture(notificationSlot)) }
            val notification = notificationSlot.captured
            val codes = listOf(
                shadowOf(notification.contentIntent).requestCode,
                shadowOf(notification.actions[0].actionIntent).requestCode,
                shadowOf(notification.actions[1].actionIntent).requestCode,
                shadowOf(notification.actions[2].actionIntent).requestCode,
                shadowOf(notification.deleteIntent).requestCode,
            )

            codes.forEach { code ->
                assertThat(seenRequestCodes.add(code)).isTrue()
            }
        }
    }
}
