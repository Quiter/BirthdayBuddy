package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import kotlinx.coroutines.CancellationException

/**
 * Executes a 2-phase transactional update across [settingsDatabase] and [appDatabase]:
 * 1. Reads the previous state via [readPreviousState].
 * 2. Writes updated data to [settingsDatabase] (Source of Truth) inside a transaction.
 * 3. Updates the [appDatabase] query cache inside a transaction.
 *
 * If updating [appDatabase] fails with a non-cancellation exception, logs the error via [logTag],
 * rolls back the [settingsDatabase] change using [rollbackSettings], and rethrows the exception.
 * [CancellationException] is always rethrown immediately to preserve structured concurrency.
 */
internal suspend fun <T> executeWithSettingsRollback(
    settingsDatabase: SettingsDatabase,
    appDatabase: AppDatabase,
    logTag: String,
    errorMessage: String,
    readPreviousState: suspend () -> T,
    writeSettings: suspend (T) -> Unit,
    rollbackSettings: suspend (T) -> Unit,
    updateAppDbCache: suspend () -> Unit,
) {
    val previousState = readPreviousState()
    settingsDatabase.withTransaction {
        writeSettings(previousState)
    }
    try {
        appDatabase.withTransaction {
            updateAppDbCache()
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e(logTag, "$errorMessage: ${e.message}", e)
        settingsDatabase.withTransaction {
            rollbackSettings(previousState)
        }
        throw e
    }
}
