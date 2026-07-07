package com.heckmannch.birthdaybuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-weites ViewModel, das auf Activity-Ebene gehalten wird.
 *
 * Verantwortlichkeiten:
 * - Hält den reaktiven [AppSettings]-State, der für das globale App-Theme benötigt wird.
 * - Triggert [NotificationRepository.syncScheduling] einmalig pro ViewModel-Lifetime
 *   (überlebt Konfigurationsänderungen wie Rotation, sodass kein redundanter
 *   syncScheduling-Aufruf bei jeder Activity-Recreation stattfindet).
 *
 * Die Activity selbst darf **nicht** direkt auf den [NotificationRepository] zugreifen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    /**
     * Reaktiver App-Settings-Flow für das globale Theme (themeMode, themeAmoled,
     * themeAccent). [SharingStarted.Eagerly] stellt sicher, dass die
     * Settings sofort beim App-Start verfügbar sind, ohne auf den ersten Collector
     * warten zu müssen – verhindert ein kurzes Theme-Flackern beim Kaltstart.
     */
    val appSettings: StateFlow<AppSettings> = notificationRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    init {
        viewModelScope.launch {
            try {
                notificationRepository.syncScheduling()
            } catch (_: Exception) {
                // Safeguard: Scheduler-Fehler dürfen den App-Start nicht blockieren.
            }
        }
    }
}
