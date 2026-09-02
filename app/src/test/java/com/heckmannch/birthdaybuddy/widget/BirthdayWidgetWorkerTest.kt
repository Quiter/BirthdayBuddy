package com.heckmannch.birthdaybuddy.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class BirthdayWidgetWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns workManager
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `calculateDelayUntilMidnight - at noon - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 12:00:00
        val now = LocalDateTime.of(2026, 7, 9, 12, 0, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(43_260_000L) // 12 hours + 1 minute
    }

    @Test
    fun `calculateDelayUntilMidnight - at midnight - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 00:00:00
        val now = LocalDateTime.of(2026, 7, 9, 0, 0, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(86_460_000L) // 24 hours + 1 minute
    }

    @Test
    fun `calculateDelayUntilMidnight - right before midnight - calculates correct delay to next day 00_01`() {
        // Arrange: 9th July 2026, 23:59:00
        val now = LocalDateTime.of(2026, 7, 9, 23, 59, 0)
        val expectedTarget = LocalDateTime.of(2026, 7, 10, 0, 1, 0)
        val expectedDelay = Duration.between(now, expectedTarget).toMillis()

        // Act
        val actualDelay = BirthdayWidgetWorker.calculateDelayUntilMidnight(now)

        // Assert
        assertThat(actualDelay).isEqualTo(expectedDelay)
        assertThat(actualDelay).isEqualTo(120_000L) // 2 minutes
    }

    @Test
    fun `enqueueNextUpdate - default policy - enqueues with KEEP`() {
        val requestSlot = slot<OneTimeWorkRequest>()

        BirthdayWidgetWorker.enqueueNextUpdate(context)

        verify {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.KEEP,
                capture(requestSlot)
            )
        }
        val capturedRequest = requestSlot.captured
        assertThat(capturedRequest.tags).contains("daily_widget_update")
    }

    @Test
    fun `enqueueNextUpdate - custom policy - enqueues with specified policy`() {
        val requestSlot = slot<OneTimeWorkRequest>()

        BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.APPEND_OR_REPLACE)

        verify {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(requestSlot)
            )
        }
    }

    @Test
    fun `doWork - success - calls widgetUpdater and enqueues next update with APPEND_OR_REPLACE`() = runTest {
        val worker = BirthdayWidgetWorker(context, workerParameters, widgetUpdater)

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "DailyWidgetUpdateSingle",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `doWork - cancellation exception - rethrows CancellationException`() = runTest {
        coEvery { widgetUpdater.updateWidget() } throws CancellationException("Job was cancelled")
        val worker = BirthdayWidgetWorker(context, workerParameters, widgetUpdater)

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                worker.doWork()
            }
        }
    }

    @Test
    fun `doWork - failure - returns retry and does not schedule next run`() = runTest {
        coEvery { widgetUpdater.updateWidget() } throws RuntimeException("Widget update crashed")
        val worker = BirthdayWidgetWorker(context, workerParameters, widgetUpdater)

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }
}
