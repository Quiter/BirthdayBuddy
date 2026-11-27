package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify as mockkVerify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext

class RepositoryTransactionHelperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsDatabase: SettingsDatabase = mock()
    private val appDatabase: AppDatabase = mock()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val executor = Executor { it.run() }
        whenever(settingsDatabase.transactionExecutor).thenReturn(executor)
        whenever(settingsDatabase.queryExecutor).thenReturn(executor)
        whenever(appDatabase.transactionExecutor).thenReturn(executor)
        whenever(appDatabase.queryExecutor).thenReturn(executor)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createTransactionElement(): CoroutineContext {
        val clazz = Class.forName("androidx.room.TransactionElement")
        val constructor = clazz.constructors.first()
        return constructor.newInstance(Dispatchers.Unconfined) as CoroutineContext
    }

    @Test
    fun executeWithSettingsRollback_completesWithoutRollback_whenCacheUpdateSucceeds() =
        runTest(createTransactionElement()) {
            val events = mutableListOf<String>()

            executeWithSettingsRollback(
                settingsDatabase = settingsDatabase,
                appDatabase = appDatabase,
                logTag = "TestTag",
                errorMessage = "Test error",
                readPreviousState = {
                    events.add("read")
                    "prev"
                },
                writeSettings = { prev ->
                    events.add("write:$prev")
                },
                rollbackSettings = { prev ->
                    events.add("rollback:$prev")
                },
                updateAppDbCache = {
                    events.add("cache")
                },
            )

            assertThat(events).containsExactly("read", "write:prev", "cache").inOrder()
            mockkVerify(exactly = 0) { Log.e(any(), any(), any()) }
        }

    @Test
    fun executeWithSettingsRollback_rollsBackSettingsAndRethrows_whenCacheUpdateFails() =
        runTest(createTransactionElement()) {
            val events = mutableListOf<String>()

            var caught: Throwable? = null
            try {
                executeWithSettingsRollback(
                    settingsDatabase = settingsDatabase,
                    appDatabase = appDatabase,
                    logTag = "TestTag",
                    errorMessage = "Cache failed",
                    readPreviousState = {
                        events.add("read")
                        "prev_state"
                    },
                    writeSettings = { prev ->
                        events.add("write:$prev")
                    },
                    rollbackSettings = { prev ->
                        events.add("rollback:$prev")
                    },
                    updateAppDbCache = {
                        events.add("cache_fail")
                        throw IllegalStateException("DB failure")
                    },
                )
            } catch (e: Throwable) {
                caught = e
            }

            assertThat(caught).isInstanceOf(IllegalStateException::class.java)
            assertThat(events).containsExactly("read", "write:prev_state", "cache_fail", "rollback:prev_state")
                .inOrder()
            mockkVerify(exactly = 1) { Log.e("TestTag", match { it.contains("Cache failed") }, any()) }
        }

    @Test
    fun executeWithSettingsRollback_rethrowsCancellationExceptionWithoutRollbackOrLogging() =
        runTest(createTransactionElement()) {
            val events = mutableListOf<String>()

            var caught: Throwable? = null
            try {
                executeWithSettingsRollback(
                    settingsDatabase = settingsDatabase,
                    appDatabase = appDatabase,
                    logTag = "TestTag",
                    errorMessage = "Cache failed",
                    readPreviousState = {
                        events.add("read")
                        "prev_state"
                    },
                    writeSettings = { prev ->
                        events.add("write:$prev")
                    },
                    rollbackSettings = { prev ->
                        events.add("rollback:$prev")
                    },
                    updateAppDbCache = {
                        events.add("cache_cancel")
                        throw CancellationException("Cancelled")
                    },
                )
            } catch (e: Throwable) {
                caught = e
            }

            assertThat(caught).isInstanceOf(CancellationException::class.java)
            assertThat(events).containsExactly("read", "write:prev_state", "cache_cancel").inOrder()
            mockkVerify(exactly = 0) { Log.e(any(), any(), any()) }
        }
}
