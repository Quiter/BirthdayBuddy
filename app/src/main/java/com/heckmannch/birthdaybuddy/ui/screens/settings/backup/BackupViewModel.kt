package com.heckmannch.birthdaybuddy.ui.screens.settings.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.usecase.ExportGiftIdeasUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.ImportGiftIdeasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportGiftIdeasUseCase: ExportGiftIdeasUseCase,
    private val importGiftIdeasUseCase: ImportGiftIdeasUseCase,
) : ViewModel() {

    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun exportGiftIdeas(
        contentResolver: ContentResolver,
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val json = exportGiftIdeasUseCase()
                withContext(ioDispatcher) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun importGiftIdeas(
        contentResolver: ContentResolver,
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onInvalid: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val json = withContext(ioDispatcher) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                }
                if (json != null) {
                    val count = importGiftIdeasUseCase(json)
                    if (count >= 0) {
                        onSuccess(count)
                    } else {
                        onInvalid()
                    }
                } else {
                    onInvalid()
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
