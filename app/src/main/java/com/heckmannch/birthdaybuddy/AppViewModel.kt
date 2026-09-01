package com.heckmannch.birthdaybuddy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.widget.BirthdayWidgetWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-weites ViewModel, das auf Activity-Ebene gehalten wird.
 *
 * Verantwortlichkeiten:
 * - Hält den reaktiven [AppSettings]-State, der für das globale App-Theme benötigt wird.
 * - Triggert [NotificationRepository.syncScheduling] sowie [BirthdayWidgetWorker.enqueueNextUpdate]
 *   einmalig pro ViewModel-Lifetime (überlebt Konfigurationsänderungen wie Rotation,
 *   sodass weder ein redundanter syncScheduling- noch ein redundanter Widget-Enqueueing-Aufruf
 *   bei jeder Activity-Recreation stattfindet).
 * - Verwaltet eingehende Intents ([pendingIntent]) in einem reaktiven StateFlow für Navigation
 *   und Deep-Links (z.B. Shortcuts, Widgets, AppFunctions) ohne Vermischung mit Activity-Lifecycle.
 *
 * Die Activity selbst darf **nicht** direkt auf Repositories oder Hintergrund-Worker zugreifen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _pendingIntent = MutableStateFlow<Intent?>(null)

    /**
     * Eingehender Intent für Navigation und Aktionen (z.B. Shortcuts, Widgets, AppFunctions).
     * Wird nach erfolgreicher Verarbeitung über [consumeIntent] auf null zurückgesetzt.
     */
    val pendingIntent: StateFlow<Intent?> = _pendingIntent.asStateFlow()

    /**
     * Übergibt einen eingehenden Intent (aus [MainActivity.onCreate] oder [MainActivity.onNewIntent])
     * zur asynchronen, sicheren Verarbeitung an die Compose-Navigationsebene.
     *
     * @param intent Der zu verarbeitende Intent.
     */
    fun handleIntent(intent: Intent?) {
        if (intent != null) {
            _pendingIntent.value = intent
        }
    }

    /**
     * Quittiert die Verarbeitung des aktuellen Intents und setzt den State zurück,
     * um eine doppelte Verarbeitung bei Recompositions oder Recreations zu verhindern.
     */
    fun consumeIntent() {
        _pendingIntent.value = null
    }

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

    /**
     * Exposes whether onboarding is completed for splash screen handling
     * and initial navigation key selection.
     */
    val onboardingCompleted: StateFlow<Boolean?> = notificationRepository.settings
        .map<AppSettings, Boolean?> { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )


    init {
        viewModelScope.launch {
            try {
                notificationRepository.syncScheduling()
            } catch (_: Exception) {
                // Safeguard: Scheduler-Fehler dürfen den App-Start nicht blockieren.
            }
        }
        try {
            BirthdayWidgetWorker.enqueueNextUpdate(context)
        } catch (_: Exception) {
            // Safeguard: Fehler beim Widget-Scheduling dürfen den App-Start nicht blockieren.
        }
    }
}
