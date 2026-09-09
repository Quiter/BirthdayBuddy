package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.domain.util.NotificationKeyUtils
import com.heckmannch.birthdaybuddy.notification.NotificationActions
import com.heckmannch.birthdaybuddy.util.AlarmScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [NotificationSchedulerImpl] verifying:
 * - [NotificationSchedulerImpl.scheduleNext]: delegates to [AlarmScheduler.scheduleNextNotificationAlarm].
 * - [NotificationSchedulerImpl.cancelNotification]: delegates to [AlarmScheduler.cancelNotificationAlarm].
 * - [NotificationSchedulerImpl.snoozeNotification]: enqueues a SnoozeWorker OneTimeWorkRequest with
 *   a 2-hour delay, WORK_TAG_SNOOZE, and correct WorkData extras (including dynamic EventType extraction).
 * - [NotificationSchedulerImpl.reshowNotification]: enqueues a SnoozeWorker OneTimeWorkRequest with
 *   the specified delay (or default 500ms) and correct WorkData extras.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSchedulerImplTest {

    private val context: Context = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)

    private lateinit var scheduler: NotificationSchedulerImpl

    @Before
    fun setUp() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager

        scheduler = NotificationSchedulerImpl(
            context = context,
            alarmScheduler = alarmScheduler,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // scheduleNext (scheduleNotificationAlarm) & cancelNotification (cancelAll)
    // =========================================================================

    @Test
    fun `scheduleNext - delegates to AlarmScheduler scheduleNextNotificationAlarm with rules`() {
        val rules = listOf(
            NotificationRule(
                daysBefore = 0,
                hour = 9,
                minute = 0
            ),
            NotificationRule(
                daysBefore = 3,
                hour = 18,
                minute = 30
            )
        )

        scheduler.scheduleNext(rules)

        verify(exactly = 1) { alarmScheduler.scheduleNextNotificationAlarm(rules) }
    }

    @Test
    fun `cancelNotification - delegates to AlarmScheduler cancelNotificationAlarm`() {
        scheduler.cancelNotification()

        verify(exactly = 1) { alarmScheduler.cancelNotificationAlarm() }
    }

    // =========================================================================
    // snoozeNotification (scheduleSnooze) Tests
    // =========================================================================

    @Test
    fun `snoozeNotification - enqueues OneTimeWorkRequest with WORK_TAG_SNOOZE, 2 hours delay, and correct data`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        val pendingId = 101
        val daysBefore = 1
        val lookupKeys = listOf("contact_lookup_key_alpha")

        scheduler.snoozeNotification(
            pendingId = pendingId,
            daysBefore = daysBefore,
            lookupKeys = lookupKeys
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        assertThat(captured.tags).contains(NotificationActions.WORK_TAG_SNOOZE)
        assertThat(captured.workSpec.initialDelay).isEqualTo(TimeUnit.HOURS.toMillis(2))

        val inputData = captured.workSpec.input
        assertThat(inputData.getInt(NotificationActions.EXTRA_PENDING_ID, -1)).isEqualTo(101)
        assertThat(inputData.getInt(NotificationActions.EXTRA_DAYS_BEFORE, -1)).isEqualTo(1)
        assertThat(inputData.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS)?.toList())
            .containsExactly("contact_lookup_key_alpha")
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.BIRTHDAY.name)
    }

    @Test
    fun `snoozeNotification - with anniversary key - extracts ANNIVERSARY event type in work data`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        val anniversaryKey = NotificationKeyUtils.encodeKey("raw_key_anniv", EventType.ANNIVERSARY)
        val lookupKeys = listOf(anniversaryKey)

        scheduler.snoozeNotification(
            pendingId = 202,
            daysBefore = 0,
            lookupKeys = lookupKeys
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        val inputData = captured.workSpec.input
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.ANNIVERSARY.name)
        assertThat(inputData.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS)?.toList())
            .containsExactly(anniversaryKey)
    }

    @Test
    fun `snoozeNotification - with name day key - extracts NAMEDAY event type in work data`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        val nameDayKey = NotificationKeyUtils.encodeKey("raw_key_nameday", EventType.NAME_DAY)
        val lookupKeys = listOf(nameDayKey)

        scheduler.snoozeNotification(
            pendingId = 303,
            daysBefore = 2,
            lookupKeys = lookupKeys
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        val inputData = captured.workSpec.input
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.NAME_DAY.name)
    }

    @Test
    fun `snoozeNotification - with empty lookupKeys list - uses BIRTHDAY as default event type`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        scheduler.snoozeNotification(
            pendingId = 404,
            daysBefore = 0,
            lookupKeys = emptyList()
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        val inputData = captured.workSpec.input
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.BIRTHDAY.name)
        assertThat(inputData.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS)).isEmpty()
    }

    // =========================================================================
    // reshowNotification Tests
    // =========================================================================

    @Test
    fun `reshowNotification - with custom delay - enqueues OneTimeWorkRequest with specified delay and data`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        val pendingId = 505
        val daysBefore = 3
        val lookupKeys = listOf("key_reshow_1", "key_reshow_2")
        val delayMillis = 2500L

        scheduler.reshowNotification(
            pendingId = pendingId,
            daysBefore = daysBefore,
            lookupKeys = lookupKeys,
            eventType = EventType.ANNIVERSARY,
            delayMillis = delayMillis
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        assertThat(captured.workSpec.initialDelay).isEqualTo(2500L)

        val inputData = captured.workSpec.input
        assertThat(inputData.getInt(NotificationActions.EXTRA_PENDING_ID, -1)).isEqualTo(505)
        assertThat(inputData.getInt(NotificationActions.EXTRA_DAYS_BEFORE, -1)).isEqualTo(3)
        assertThat(inputData.getStringArray(NotificationActions.EXTRA_LOOKUP_KEYS)?.toList())
            .containsExactly("key_reshow_1", "key_reshow_2")
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.ANNIVERSARY.name)
    }

    @Test
    fun `reshowNotification - with default delayMillis parameter - enqueues with 500ms delay`() {
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workRequestSlot)) } returns mockk(relaxed = true)

        scheduler.reshowNotification(
            pendingId = 606,
            daysBefore = 0,
            lookupKeys = listOf("key_default_delay"),
            eventType = EventType.BIRTHDAY
        )

        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workRequestSlot.captured
        assertThat(captured.workSpec.initialDelay).isEqualTo(500L)

        val inputData = captured.workSpec.input
        assertThat(inputData.getInt(NotificationActions.EXTRA_PENDING_ID, -1)).isEqualTo(606)
        assertThat(inputData.getString(NotificationActions.EXTRA_EVENT_TYPE)).isEqualTo(EventType.BIRTHDAY.name)
    }
}
