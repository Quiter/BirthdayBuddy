package com.heckmannch.birthdaybuddy.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimeRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)

    private class FakeClock(
        var baseTimeMillis: Long,
        override var zone: ZoneId = ZoneId.of("Europe/Berlin"),
        private val elapsedMillis: () -> Long = { 0L },
    ) : Clock {
        override fun currentTimeMillis(): Long = baseTimeMillis + elapsedMillis()
    }

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        mockkConstructor(IntentFilter::class)
        every { anyConstructed<IntentFilter>().addAction(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun currentDate_shouldEmitCurrentDateInitiallyAndRegisterReceiver() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        val filterSlot = slot<IntentFilter>()
        val initialZoned = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val clock = FakeClock(initialZoned.toInstant().toEpochMilli(), initialZoned.zone)

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                capture(filterSlot),
                ContextCompat.RECEIVER_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val initialDate = repository.currentDate.first()

        assertThat(initialDate).isEqualTo(LocalDate.of(2026, 6, 15))
        verify(exactly = 1) {
            ContextCompat.registerReceiver(
                context,
                receiverSlot.captured,
                filterSlot.captured,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        assertThat(receiverSlot.isCaptured).isTrue()
        assertThat(filterSlot.isCaptured).isTrue()
    }

    @Test
    fun currentDate_shouldUnregisterReceiverAfterStopTimeoutWhenAllCollectorsCancel() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        val initialZoned = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val clock = FakeClock(initialZoned.toInstant().toEpochMilli(), initialZoned.zone)

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                any(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect {}
        }
        runCurrent()

        verify(exactly = 1) {
            ContextCompat.registerReceiver(
                context,
                receiverSlot.captured,
                any(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }

        job.cancel()
        // Within the 5s WhileSubscribed grace period the receiver stays registered
        advanceTimeBy(4_999)
        runCurrent()
        verify(exactly = 0) { context.unregisterReceiver(any()) }

        // After 5s timeout it unregisters cleanly
        advanceTimeBy(1)
        runCurrent()
        verify(exactly = 1) { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun currentDate_shouldShareSingleReceiverAcrossMultipleCollectors() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        val initialZoned = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val clock = FakeClock(initialZoned.toInstant().toEpochMilli(), initialZoned.zone)

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                any(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val dates1 = mutableListOf<LocalDate>()
        val dates2 = mutableListOf<LocalDate>()

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect { dates1.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect { dates2.add(it) }
        }
        runCurrent()

        verify(exactly = 1) {
            ContextCompat.registerReceiver(
                context,
                any(),
                any(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        assertThat(dates1).containsExactly(LocalDate.of(2026, 6, 15))
        assertThat(dates2).containsExactly(LocalDate.of(2026, 6, 15))

        // Cancelling only the first collector must keep the shared receiver active
        job1.cancel()
        advanceTimeBy(6_000)
        runCurrent()
        verify(exactly = 0) { context.unregisterReceiver(any()) }

        job2.cancel()
        advanceTimeBy(5_000)
        runCurrent()
        verify(exactly = 1) { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun currentDate_shouldEmitNewDateWhenBroadcastReceiverIsTriggered() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        val initialZoned = ZonedDateTime.of(2026, 6, 15, 23, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val clock = FakeClock(initialZoned.toInstant().toEpochMilli(), initialZoned.zone)

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                any(),
                ContextCompat.RECEIVER_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val collectedDates = mutableListOf<LocalDate>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect { collectedDates.add(it) }
        }
        runCurrent()

        assertThat(collectedDates).containsExactly(LocalDate.of(2026, 6, 15))

        // Advance clock to next day and trigger system date-changed broadcast
        clock.baseTimeMillis = ZonedDateTime.of(2026, 6, 16, 0, 0, 0, 0, initialZoned.zone)
            .toInstant()
            .toEpochMilli()
        receiverSlot.captured.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))
        runCurrent()

        assertThat(collectedDates).containsExactly(
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 6, 16),
        ).inOrder()

        job.cancel()
    }

    @Test
    fun currentDate_dstSpringForward_emitsNextDateAtExact23HourMidnight() = runTest {
        val zone = ZoneId.of("Europe/Berlin")
        // 2026-03-29 switches from CET (+01:00) to CEST (+02:00): 23-hour day.
        // 00:30 to 24:00 (2026-03-30 00:00) is 22h 30m = 81_000_000 ms (+ 1_000 ms buffer).
        val start = ZonedDateTime.of(2026, 3, 29, 0, 30, 0, 0, zone)
        val clock = FakeClock(
            baseTimeMillis = start.toInstant().toEpochMilli(),
            zone = zone,
            elapsedMillis = { testScheduler.currentTime },
        )

        every {
            ContextCompat.registerReceiver(context, any(), any(), ContextCompat.RECEIVER_EXPORTED)
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val collectedDates = mutableListOf<LocalDate>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect { collectedDates.add(it) }
        }
        runCurrent()

        assertThat(collectedDates).containsExactly(LocalDate.of(2026, 3, 29))

        advanceTimeBy(81_000_999L)
        runCurrent()
        assertThat(collectedDates).containsExactly(LocalDate.of(2026, 3, 29))

        advanceTimeBy(1L)
        runCurrent()
        assertThat(collectedDates).containsExactly(
            LocalDate.of(2026, 3, 29),
            LocalDate.of(2026, 3, 30),
        ).inOrder()

        job.cancel()
    }

    @Test
    fun currentDate_dstFallBack_waitsFull25HoursBeforeEmittingNextDate() = runTest {
        val zone = ZoneId.of("Europe/Berlin")
        // 2026-10-25 switches from CEST (+02:00) to CET (+01:00): 25-hour day.
        // 00:30 to 24:00 (2026-10-26 00:00) is 24h 30m = 88_200_000 ms (+ 1_000 ms buffer).
        val start = ZonedDateTime.of(2026, 10, 25, 0, 30, 0, 0, zone)
        val clock = FakeClock(
            baseTimeMillis = start.toInstant().toEpochMilli(),
            zone = zone,
            elapsedMillis = { testScheduler.currentTime },
        )

        every {
            ContextCompat.registerReceiver(context, any(), any(), ContextCompat.RECEIVER_EXPORTED)
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val collectedDates = mutableListOf<LocalDate>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect { collectedDates.add(it) }
        }
        runCurrent()

        assertThat(collectedDates).containsExactly(LocalDate.of(2026, 10, 25))

        // At 23h 30m + 1s (what a non-DST 24h calculation would have used), it must NOT emit next day yet
        advanceTimeBy(84_601_000L)
        runCurrent()
        assertThat(collectedDates).containsExactly(LocalDate.of(2026, 10, 25))

        // Advance the remaining 1 hour (3_600_000 ms) to reach actual midnight + 1s
        advanceTimeBy(3_600_000L)
        runCurrent()
        assertThat(collectedDates).containsExactly(
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 10, 26),
        ).inOrder()

        job.cancel()
    }

    @Test
    fun currentDate_doesNotReplayStaleDateAfterStopTimeoutExpires() = runTest {
        val zone = ZoneId.of("Europe/Berlin")
        val day1 = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, zone)
        val clock = FakeClock(day1.toInstant().toEpochMilli(), zone)

        every {
            ContextCompat.registerReceiver(context, any(), any(), ContextCompat.RECEIVER_EXPORTED)
        } returns null

        val repository = TimeRepositoryImpl(context, clock, backgroundScope)
        val firstJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.currentDate.collect {}
        }
        firstJob.cancel()

        // Let WhileSubscribed stop timeout expire
        advanceTimeBy(5_000L)
        runCurrent()

        // Advance clock to next day while unsubscribed
        clock.baseTimeMillis = ZonedDateTime.of(2026, 6, 16, 8, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

        val nextDate = repository.currentDate.first()
        assertThat(nextDate).isEqualTo(LocalDate.of(2026, 6, 16))
    }
}