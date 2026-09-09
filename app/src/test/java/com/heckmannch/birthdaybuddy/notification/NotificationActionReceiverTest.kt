package com.heckmannch.birthdaybuddy.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.repository.NotificationSchedulerImpl
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.DismissNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.MarkNotificationAsDoneUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ReshowNotificationUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SnoozeNotificationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [NotificationActionReceiver] verifying handling of notification actions:
 * - [NotificationActions.ACTION_DONE]: cancels notification and marks pending notification as completed.
 * - [NotificationActions.ACTION_SNOOZE]: cancels notification and enqueues [SnoozeWorker] with a 2-hour delay.
 * - [NotificationActions.ACTION_DISMISSED]: increments dismiss count and triggers reshowing for active notifications.
 * - Invalid, unknown, or null actions: early return without triggering side effects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationActionReceiverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val markNotificationAsDoneUseCase = mockk<MarkNotificationAsDoneUseCase>(relaxed = true)
    private val dismissNotificationUseCase = mockk<DismissNotificationUseCase>(relaxed = true)
    private val snoozeNotificationUseCase = mockk<SnoozeNotificationUseCase>(relaxed = true)
    private val reshowNotificationUseCase = mockk<ReshowNotificationUseCase>(relaxed = true)
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)

    private lateinit var receiver: NotificationActionReceiver

    @Before
    fun setUp() {
        val baseContext = RuntimeEnvironment.getApplication()
        context = spyk(baseContext) {
            every { getSystemService(NotificationManager::class.java) } returns notificationManager
            every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        }

        receiver = spyk(NotificationActionReceiver())
        val injectedField = Hilt_NotificationActionReceiver::class.java.getDeclaredField("injected")
        injectedField.isAccessible = true
        injectedField.set(receiver, true)

        every { receiver.goAsync() } returns pendingResult

        receiver.applicationScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        receiver.notificationRepository = notificationRepository
        receiver.markNotificationAsDoneUseCase = markNotificationAsDoneUseCase
        receiver.dismissNotificationUseCase = dismissNotificationUseCase
        receiver.snoozeNotificationUseCase = snoozeNotificationUseCase
        receiver.reshowNotificationUseCase = reshowNotificationUseCase
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // 1. ACTION_DONE Tests
    // =========================================================================

    @Test
    fun onReceive_actionDone_cancelsNotificationAndMarksPendingNotificationAsDone() = runTest {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DONE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 202)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 15)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { notificationManager.cancel(202) }
        coVerify(exactly = 1) { markNotificationAsDoneUseCase(15) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDone_withPendingIdMinusOne_cancelsNotificationWithoutCallingRepository() {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DONE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 202)
            putExtra(NotificationActions.EXTRA_PENDING_ID, -1)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { notificationManager.cancel(202) }
        coVerify(exactly = 0) { markNotificationAsDoneUseCase(any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun onReceive_actionDone_whenRepositoryThrowsException_finishesPendingResultWithoutThrowing() = runTest {
        coEvery { markNotificationAsDoneUseCase(15) } throws RuntimeException("Database error")

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DONE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 202)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 15)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { notificationManager.cancel(202) }
        coVerify(exactly = 1) { markNotificationAsDoneUseCase(15) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    // =========================================================================
    // 2. ACTION_SNOOZE Tests
    // =========================================================================

    @Test
    fun onReceive_actionSnooze_cancelsNotificationAndDelegatesToSnoozeUseCase() {
        val lookupKeys = arrayOf("contact_lookup_1", "contact_lookup_2")
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_SNOOZE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 101)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 12)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 3)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, lookupKeys)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { notificationManager.cancel(101) }
        verify(exactly = 1) {
            snoozeNotificationUseCase(
                pendingId = 12,
                daysBefore = 3,
                lookupKeys = listOf("contact_lookup_1", "contact_lookup_2")
            )
        }
    }

    @Test
    fun onReceive_actionSnooze_enqueuesSnoozeWorkerWithCorrectDelay() {
        val workManager = mockk<WorkManager>(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        val realScheduler = NotificationSchedulerImpl(context, mockk(relaxed = true))
        receiver.snoozeNotificationUseCase = SnoozeNotificationUseCase(realScheduler)

        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        val lookupKeys = arrayOf("contact_lookup_1")
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_SNOOZE
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 101)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 12)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 3)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, lookupKeys)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { notificationManager.cancel(101) }
        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val capturedRequest = workRequestSlot.captured
        assertThat(capturedRequest.tags).contains(NotificationActions.WORK_TAG_SNOOZE)
        assertThat(capturedRequest.workSpec.initialDelay).isEqualTo(TimeUnit.HOURS.toMillis(2))
        assertThat(capturedRequest.workSpec.input.getInt(NotificationActions.EXTRA_PENDING_ID, -1)).isEqualTo(12)
        assertThat(capturedRequest.workSpec.input.getInt(NotificationActions.EXTRA_DAYS_BEFORE, -1)).isEqualTo(3)
        assertThat(capturedRequest.workSpec.input.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS)?.toList())
            .containsExactly("contact_lookup_1")
    }

    // =========================================================================
    // 3. ACTION_DISMISSED Tests
    // =========================================================================

    @Test
    fun onReceive_actionDismissed_whenNotificationIsStillActive_incrementsDismissCountAndReshows() = runTest {
        val pendingNotification = PendingNotification(
            contactLookupKeys = listOf("key_birthday"),
            daysBefore = 0,
            year = 2026,
            isDone = false,
            dismissCount = 1
        )
        coEvery { notificationRepository.getPendingNotificationById(30) } returns pendingNotification

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 203)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 30)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 0)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("key_birthday"))
        }

        receiver.onReceive(context, intent)

        coVerifyOrder {
            dismissNotificationUseCase(30)
            notificationRepository.getPendingNotificationById(30)
        }
        verify(exactly = 1) {
            reshowNotificationUseCase(
                pendingId = 30,
                daysBefore = 0,
                lookupKeys = listOf("key_birthday"),
                eventType = EventType.BIRTHDAY
            )
        }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDismissed_extractsAnniversaryEventTypeCorrectly() = runTest {
        val pendingNotification = PendingNotification(
            contactLookupKeys = listOf("anniversary:key_anniv"),
            daysBefore = 3,
            year = 2026,
            isDone = false,
            dismissCount = 1
        )
        coEvery { notificationRepository.getPendingNotificationById(31) } returns pendingNotification

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 204)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 31)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 3)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("anniversary:key_anniv"))
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            reshowNotificationUseCase(
                pendingId = 31,
                daysBefore = 3,
                lookupKeys = listOf("anniversary:key_anniv"),
                eventType = EventType.ANNIVERSARY
            )
        }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDismissed_extractsNameDayEventTypeCorrectly() = runTest {
        val pendingNotification = PendingNotification(
            contactLookupKeys = listOf("nameday:key_nameday"),
            daysBefore = 1,
            year = 2026,
            isDone = false,
            dismissCount = 1
        )
        coEvery { notificationRepository.getPendingNotificationById(35) } returns pendingNotification

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 209)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 35)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 1)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("nameday:key_nameday"))
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            reshowNotificationUseCase(
                pendingId = 35,
                daysBefore = 1,
                lookupKeys = listOf("nameday:key_nameday"),
                eventType = EventType.NAME_DAY
            )
        }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDismissed_whenNotificationIsAlreadyDone_incrementsDismissCountButDoesNotReshow() = runTest {
        val pendingNotification = PendingNotification(
            contactLookupKeys = listOf("key_birthday"),
            daysBefore = 0,
            year = 2026,
            isDone = true,
            dismissCount = 1
        )
        coEvery { notificationRepository.getPendingNotificationById(32) } returns pendingNotification

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 205)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 32)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 0)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("key_birthday"))
        }

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { dismissNotificationUseCase(32) }
        verify(exactly = 0) { reshowNotificationUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDismissed_whenPendingNotificationNotFound_incrementsDismissCountButDoesNotReshow() = runTest {
        coEvery { notificationRepository.getPendingNotificationById(33) } returns null

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 206)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 33)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 0)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("key_birthday"))
        }

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { dismissNotificationUseCase(33) }
        verify(exactly = 0) { reshowNotificationUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun onReceive_actionDismissed_withPendingIdMinusOne_doesNothing() {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 207)
            putExtra(NotificationActions.EXTRA_PENDING_ID, -1)
        }

        receiver.onReceive(context, intent)

        coVerify(exactly = 0) { dismissNotificationUseCase(any()) }
        coVerify(exactly = 0) { notificationRepository.getPendingNotificationById(any()) }
        verify(exactly = 0) { reshowNotificationUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun onReceive_actionDismissed_whenRepositoryThrowsException_finishesPendingResultWithoutThrowing() = runTest {
        coEvery { dismissNotificationUseCase(34) } throws RuntimeException("Database error")

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActions.ACTION_DISMISSED
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 208)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 34)
            putExtra(NotificationActions.EXTRA_DAYS_BEFORE, 0)
            putExtra(NotificationActions.EXTRA_LOOKUP_KEYS, arrayOf("key_birthday"))
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) { pendingResult.finish() }
    }

    // =========================================================================
    // 4. Invalid Actions / Null Inputs Tests
    // =========================================================================

    @Test
    fun onReceive_withNullContext_returnsEarlyWithoutSideEffects() {
        val intent = Intent(NotificationActions.ACTION_DONE)
        receiver.onReceive(null, intent)

        verify(exactly = 0) { notificationManager.cancel(any()) }
        coVerify(exactly = 0) { markNotificationAsDoneUseCase(any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun onReceive_withNullIntent_returnsEarlyWithoutSideEffects() {
        receiver.onReceive(context, null)

        verify(exactly = 0) { notificationManager.cancel(any()) }
        coVerify(exactly = 0) { markNotificationAsDoneUseCase(any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun onReceive_withUnknownAction_returnsEarlyWithoutSideEffects() {
        val intent = Intent("com.heckmannch.birthdaybuddy.action.UNKNOWN").apply {
            putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, 123)
            putExtra(NotificationActions.EXTRA_PENDING_ID, 456)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 0) { notificationManager.cancel(any()) }
        coVerify(exactly = 0) { markNotificationAsDoneUseCase(any()) }
        coVerify(exactly = 0) { dismissNotificationUseCase(any()) }
        verify(exactly = 0) { snoozeNotificationUseCase(any(), any(), any()) }
        verify(exactly = 0) { reshowNotificationUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun onReceive_withNullAction_returnsEarlyWithoutSideEffects() {
        val intent = Intent() // Null action

        receiver.onReceive(context, intent)

        verify(exactly = 0) { notificationManager.cancel(any()) }
        coVerify(exactly = 0) { markNotificationAsDoneUseCase(any()) }
        coVerify(exactly = 0) { dismissNotificationUseCase(any()) }
        verify(exactly = 0) { snoozeNotificationUseCase(any(), any(), any()) }
        verify(exactly = 0) { reshowNotificationUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }
}
