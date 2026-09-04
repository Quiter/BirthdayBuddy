package com.heckmannch.birthdaybuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class BootReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val appContext = mockk<Context>(relaxed = true)
    private val entryPoint = mockk<BootReceiver.BootReceiverEntryPoint>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val widgetUpdater = mockk<WidgetUpdater>(relaxed = true)
    private val pendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
    private val testScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)

    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        receiver = spyk(BootReceiver())
        every { receiver.goAsync() } returns pendingResult
        every { context.applicationContext } returns appContext

        mockkObject(BirthdayWidgetWorker.Companion)
        every { BirthdayWidgetWorker.enqueueNextUpdate(any(), any()) } returns Unit

        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(
                appContext,
                BootReceiver.BootReceiverEntryPoint::class.java
            )
        } returns entryPoint

        every { entryPoint.applicationScope() } returns testScope
        every { entryPoint.notificationRepository() } returns notificationRepository
        every { entryPoint.widgetUpdater() } returns widgetUpdater
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createIntent(action: String?): Intent {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns action
        return intent
    }

    @Test
    fun `onReceive with BOOT_COMPLETED syncs scheduling, updates widget, and finishes pending result`() {
        val intent = createIntent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with MY_PACKAGE_REPLACED syncs scheduling, updates widget, and finishes pending result`() {
        val intent = createIntent(Intent.ACTION_MY_PACKAGE_REPLACED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with TIMEZONE_CHANGED syncs scheduling, updates widget, and finishes pending result`() {
        val intent = createIntent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with TIME_SET syncs scheduling, updates widget, and finishes pending result`() {
        val intent = createIntent(Intent.ACTION_TIME_CHANGED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with DATE_CHANGED syncs scheduling, updates widget, and finishes pending result`() {
        val intent = createIntent(Intent.ACTION_DATE_CHANGED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive with unsupported action ignores broadcast and does not sync scheduling or update widget`() {
        val intent = createIntent(Intent.ACTION_AIRPLANE_MODE_CHANGED)

        receiver.onReceive(context, intent)

        coVerify(exactly = 0) { notificationRepository.syncScheduling() }
        coVerify(exactly = 0) { widgetUpdater.updateWidget() }
        verify(exactly = 0) { BirthdayWidgetWorker.enqueueNextUpdate(any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive with null context or null intent does nothing`() {
        receiver.onReceive(null, createIntent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context, null)
        receiver.onReceive(context, createIntent(null))

        coVerify(exactly = 0) { notificationRepository.syncScheduling() }
        coVerify(exactly = 0) { widgetUpdater.updateWidget() }
        verify(exactly = 0) { BirthdayWidgetWorker.enqueueNextUpdate(any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles EntryPoint resolution failure gracefully`() {
        every {
            EntryPointAccessors.fromApplication(
                appContext,
                BootReceiver.BootReceiverEntryPoint::class.java
            )
        } throws IllegalStateException("Hilt entry point not found")

        val intent = createIntent(Intent.ACTION_TIMEZONE_CHANGED)
        receiver.onReceive(context, intent)

        coVerify(exactly = 0) { notificationRepository.syncScheduling() }
        coVerify(exactly = 0) { widgetUpdater.updateWidget() }
        verify(exactly = 0) { BirthdayWidgetWorker.enqueueNextUpdate(any(), any()) }
        verify(exactly = 0) { receiver.goAsync() }
    }

    @Test
    fun `onReceive handles syncScheduling exception gracefully and still updates widget and finishes pending result`() {
        coEvery { notificationRepository.syncScheduling() } throws RuntimeException("Scheduler error")

        val intent = createIntent(Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { BirthdayWidgetWorker.enqueueNextUpdate(context, ExistingWorkPolicy.REPLACE) }
        verify(exactly = 1) { pendingResult.finish() }
    }

    @Test
    fun `onReceive handles widget update exception gracefully and still syncs scheduling and finishes pending result`() {
        coEvery { widgetUpdater.updateWidget() } throws RuntimeException("Widget update error")

        val intent = createIntent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        coVerify(exactly = 1) { notificationRepository.syncScheduling() }
        coVerify(exactly = 1) { widgetUpdater.updateWidget() }
        verify(exactly = 1) { pendingResult.finish() }
    }
}
