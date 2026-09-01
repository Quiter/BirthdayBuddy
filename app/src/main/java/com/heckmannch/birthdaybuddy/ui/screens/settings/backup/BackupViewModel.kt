package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Backup screen.
 * Orchestrates export and import intents, and delegates all Android framework details
 * (such as ContentResolver operations) to the Use Case and Repository layers.
 *
 * Design:
 * - Decoupled from Android's ContentResolver APIs.
 * - Follows the Uni-Directional Data Flow (UDF) / MVI pattern via [onIntent].
 * - Injects the I/O dispatcher using the custom Hilt [@IoDispatcher] qualifier.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportGiftIdeasUseCase: ExportGiftIdeasUseCase,
    private val importGiftIdeasUseCase: ImportGiftIdeasUseCase,
    @param:IoDispatcher @Suppress("unused") private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /**
     * Central entry point to process user intents/actions.
     *
     * @param intent The MVI intent to handle.
     */
    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.ExportBackup -> exportGiftIdeas(
                uri = intent.uri,
                onSuccess = intent.onSuccess,
                onError = intent.onError
            )

            is BackupIntent.ImportBackup -> importGiftIdeas(
                uri = intent.uri,
                onSuccess = intent.onSuccess,
                onInvalid = intent.onInvalid,
                onError = intent.onError
            )
        }
    }

    private fun exportGiftIdeas(
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                exportGiftIdeasUseCase(uri)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun importGiftIdeas(
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onInvalid: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val count = importGiftIdeasUseCase(uri)
                if (count >= 0) {
                    onSuccess(count)
                } else {
                    onInvalid()
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}

/**
 * Sealed interface representing all MVI intents (actions) that can be sent
 * from the view/composable to the [BackupViewModel].
 */
sealed interface BackupIntent {
    /**
     * Intent to export all gift ideas to the given destination [uri].
     */
    data class ExportBackup(
        val uri: Uri,
        val onSuccess: () -> Unit,
        val onError: (Exception) -> Unit
    ) : BackupIntent

    /**
     * Intent to import gift ideas from the given source [uri].
     */
    data class ImportBackup(
        val uri: Uri,
        val onSuccess: (Int) -> Unit,
        val onInvalid: () -> Unit,
        val onError: (Exception) -> Unit
    ) : BackupIntent
}
