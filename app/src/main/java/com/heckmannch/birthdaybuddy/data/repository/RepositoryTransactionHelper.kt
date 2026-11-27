package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Executes a 2-phase transactional update across [settingsDatabase] and [appDatabase]:
 * 1. Atomically reads the previous state via [readPreviousState] and writes updated data to
 *    [settingsDatabase] (Source of Truth) inside a single transaction.
 * 2. Updates the [appDatabase] query cache inside a transaction, receiving the previously read state.
 *
 * If updating [appDatabase] fails (including via [CancellationException]), rolls back the
 * [settingsDatabase] change using [rollbackSettings] inside [NonCancellable] so both databases
 * remain consistent, and rethrows the original exception. Non-cancellation failures are logged
 * via [logTag].
 */
internal suspend fun <T> executeWithSettingsRollback(
    settingsDatabase: SettingsDatabase,
    appDatabase: AppDatabase,
    logTag: String,
    errorMessage: String,
    readPreviousState: suspend () -> T,
    writeSettings: suspend (T) -> Unit,
    rollbackSettings: suspend (T) -> Unit,
    updateAppDbCache: suspend (T) -> Unit,
) {
    val previousState = settingsDatabase.withTransaction {
        val prev = readPreviousState()
        writeSettings(prev)
        prev
    }
    try {
        appDatabase.withTransaction {
            updateAppDbCache(previousState)
        }
    } catch (e: CancellationException) {
        try {
            withContext(NonCancellable) {
                settingsDatabase.withTransaction {
                    rollbackSettings(previousState)
                }
            }
        } catch (rollbackError: Exception) {
            Log.e(logTag, "Rollback failed after cancellation ($errorMessage): ${rollbackError.message}", rollbackError)
            e.addSuppressed(rollbackError)
        }
        throw e
    } catch (e: Exception) {
        Log.e(logTag, "$errorMessage: ${e.message}", e)
        try {
            withContext(NonCancellable) {
                settingsDatabase.withTransaction {
                    rollbackSettings(previousState)
                }
            }
        } catch (rollbackError: Exception) {
            Log.e(logTag, "Rollback failed ($errorMessage): ${rollbackError.message}", rollbackError)
            e.addSuppressed(rollbackError)
        }
        throw e
    }
}
