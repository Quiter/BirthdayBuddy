package com.heckmannch.birthdaybuddy.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
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
                val json = contactRepository.exportGiftIdeas()
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
                    val count = contactRepository.importGiftIdeas(json)
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
