package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
) : ViewModel() {

    suspend fun exportGiftIdeas() = contactRepository.exportGiftIdeas()

    suspend fun importGiftIdeas(json: String): Int {
        return contactRepository.importGiftIdeas(json)
    }
}
