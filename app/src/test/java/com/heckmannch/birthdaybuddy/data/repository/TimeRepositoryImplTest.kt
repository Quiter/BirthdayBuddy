package com.heckmannch.birthdaybuddy.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TimeRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun currentDate_shouldEmitCurrentDateInitiallyAndRegisterReceiver() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        val filterSlot = slot<IntentFilter>()

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                capture(filterSlot),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context)
        val initialDate = repository.currentDate.first()

        assertThat(initialDate).isEqualTo(LocalDate.now())
        verify {
            ContextCompat.registerReceiver(
                context,
                receiverSlot.captured,
                filterSlot.captured,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        assertThat(receiverSlot.isCaptured).isTrue()
        assertThat(filterSlot.isCaptured).isTrue()
    }

    @Test
    fun currentDate_shouldUnregisterReceiverWhenFlowCollectionIsCancelled() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context)
        val job = launch(UnconfinedTestDispatcher()) {
            repository.currentDate.collect {}
        }

        verify {
            ContextCompat.registerReceiver(
                context,
                receiverSlot.captured,
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        job.cancel()

        verify { context.unregisterReceiver(receiverSlot.captured) }
    }

    @Test
    fun currentDate_shouldEmitNewDateWhenBroadcastReceiverIsTriggered() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()

        every {
            ContextCompat.registerReceiver(
                context,
                capture(receiverSlot),
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } returns null

        val repository = TimeRepositoryImpl(context)
        val collectedDates = mutableListOf<LocalDate>()

        val job = launch(UnconfinedTestDispatcher()) {
            repository.currentDate.collect { collectedDates.add(it) }
        }

        verify {
            ContextCompat.registerReceiver(
                context,
                receiverSlot.captured,
                any(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        val receiver = receiverSlot.captured

        // Trigger onReceive
        receiver.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))

        assertThat(collectedDates).isNotEmpty()
        assertThat(collectedDates.first()).isEqualTo(LocalDate.now())

        job.cancel()
    }
}