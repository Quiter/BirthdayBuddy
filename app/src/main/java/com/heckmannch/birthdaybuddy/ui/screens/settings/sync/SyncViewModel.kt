package com.heckmannch.birthdaybuddy.ui.screens.settings.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the Sync Settings screen.
 * Handles manual contact synchronization operations independently from HomeViewModel.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _syncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)
    val syncCompletedEvent: SharedFlow<Unit> = _syncCompletedEvent.asSharedFlow()

    fun syncContacts() {
        viewModelScope.launch {
            contactRepository.clearIgnoredCouplePairs()
            val startTime = clock.currentTimeMillis()
            contactRepository.syncContacts()
            val elapsedTime = clock.currentTimeMillis() - startTime
            if (elapsedTime < 800) {
                delay((800 - elapsedTime).milliseconds)
            }
            _syncCompletedEvent.emit(Unit)
        }
    }
}
