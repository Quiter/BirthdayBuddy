package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.widget.BirthdayWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
) : ViewModel() {

    suspend fun exportGiftIdeas() = contactRepository.exportGiftIdeas()

    suspend fun importGiftIdeas(json: String): Int {
        val count = contactRepository.importGiftIdeas(json)
        if (count > 0) updateWidget()
        return count
    }

    private fun updateWidget() = viewModelScope.launch {
        try {
            BirthdayWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("BackupViewModel", "Widget update failed", e)
        }
    }
}
