package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.ui.model.BackupMessage
import com.heckmannch.birthdaybuddy.ui.model.BackupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Backup screen.
 * Orchestrates export and import intents, and delegates all Android framework details
 * (such as ContentResolver operations) to the Use Case and Repository layers.
 *
 * Design:
 * - Decoupled from Android's ContentResolver APIs.
 * - Follows the Uni-Directional Data Flow (UDF) / MVI pattern via [onIntent] and [uiState].
 * - Injects the I/O dispatcher using the custom Hilt [@IoDispatcher] qualifier.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportGiftIdeasUseCase: ExportGiftIdeasUseCase,
    private val importGiftIdeasUseCase: ImportGiftIdeasUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /**
     * Central entry point to process user intents/actions.
     *
     * @param intent The MVI intent to handle.
     */
    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.ExportGiftIdeas -> exportGiftIdeas(intent.uriString)
            is BackupIntent.ImportGiftIdeas -> importGiftIdeas(intent.uriString)
            BackupIntent.ClearMessage -> clearMessage()
        }
    }

    private fun exportGiftIdeas(uriString: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(ioDispatcher) {
                    exportGiftIdeasUseCase(uriString)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = BackupMessage.ExportSuccess
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = BackupMessage.ExportError(e.message)
                    )
                }
            }
        }
    }

    private fun importGiftIdeas(uriString: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val count = withContext(ioDispatcher) {
                    importGiftIdeasUseCase(uriString)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (count >= 0) {
                            BackupMessage.ImportSuccess(count)
                        } else {
                            BackupMessage.ImportInvalid
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = BackupMessage.ImportError(e.message)
                    )
                }
            }
        }
    }

    private fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
