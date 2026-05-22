package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    suspend fun exportGiftIdeas() = contactRepository.exportGiftIdeas()

    suspend fun importGiftIdeas(json: String): Int {
        val count = contactRepository.importGiftIdeas(json)
        if (count > 0) updateWidget()
        return count
    }

    private fun updateWidget() = viewModelScope.launch {
        widgetUpdater.updateWidget()
    }
}
