package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class NotificationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        io.mockk.mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleNext - empty rules list - cancels unique work`() {
        // Act
        NotificationWorker.scheduleNext(context, emptyList())

        // Assert
        verify { workManager.cancelUniqueWork("FlexibleNotificationUpdate") }
    }

    @Test
    fun `scheduleNext - rule in future today - schedules correct delay`() {
        // Arrange: mock LocalDateTime.now() to 12:00:00
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.of(2026, 7, 9, 12, 0, 0)

        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 15, minute = 0), // 3 hours in future
            NotificationRule(id = 2, daysBefore = 0, hour = 8, minute = 0)   // in the past today
        )

        val requestSlot = slot<OneTimeWorkRequest>()

        // Act
        NotificationWorker.scheduleNext(context, rules, forceReplace = true)

        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.REPLACE,
                capture(requestSlot)
            )
        }

        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.tags).contains("birthday_notification")
        // Expected delay is from 12:00:00 to 15:00:00 today = 3 hours = 10,800,000 ms
        assertThat(capturedRequest.workSpec.initialDelay).isEqualTo(10_800_000L)
    }

    @Test
    fun `scheduleNext - all rules in past today - schedules first rule for tomorrow`() {
        // Arrange: mock LocalDateTime.now() to 12:00:00
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns LocalDateTime.of(2026, 7, 9, 12, 0, 0)

        val rules = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 10, minute = 0), // past
            NotificationRule(id = 2, daysBefore = 0, hour = 8, minute = 0)   // past
        )

        val requestSlot = slot<OneTimeWorkRequest>()

        // Act
        NotificationWorker.scheduleNext(context, rules, forceReplace = false)

        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "FlexibleNotificationUpdate",
                ExistingWorkPolicy.KEEP,
                capture(requestSlot)
            )
        }

        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.tags).contains("birthday_notification")
        // Earliest unique rule is 08:00 (since 08:00 < 10:00).
        // Target is 2026-07-10T08:00:00.
        // Delay from 2026-07-09T12:00:00 to 2026-07-10T08:00:00 is 20 hours = 72,000,000 ms
        assertThat(capturedRequest.workSpec.initialDelay).isEqualTo(72_000_000L)
    }
}
