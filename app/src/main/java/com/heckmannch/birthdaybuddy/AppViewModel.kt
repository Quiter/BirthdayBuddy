package com.heckmannch.birthdaybuddy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.ui.navigation.AppAction
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * - Hält den reaktiven [AppSettings]-State (bezogen aus [SettingsRepository]), der für das globale App-Theme benötigt wird.
 * - Triggert [NotificationRepository.syncScheduling] sowie [WidgetUpdater.scheduleDailyUpdate]
 *   einmalig pro ViewModel-Lifetime (überlebt Konfigurationsänderungen wie Rotation,
 *   sodass weder ein redundanter syncScheduling- noch ein redundanter Widget-Scheduling-Aufruf
 *   bei jeder Activity-Recreation stattfindet).
 * - Widget-Update-Strategie (Single-Path): [WidgetUpdater.scheduleDailyUpdate] delegiert an den
 *   AlarmScheduler, um den deterministischen Mitternachts-Alarm via AlarmManager sicherzustellen.
 *   Beim App-Start wird bewusst kein redundanter WorkManager-Job eingeplant.
 * - Verwaltet eingehende Aktionen ([pendingAction]) in einem reaktiven StateFlow für Navigation
 *   und Deep-Links (z.B. Shortcuts, Widgets, AppFunctions) ohne Vermischung mit Activity-Lifecycle
 *   oder Abhängigkeit zu Android-Framework-APIs wie [android.content.Intent].
 *
 * Die Activity selbst darf **nicht** direkt auf Repositories oder Hintergrund-Worker zugreifen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    settingsRepository: SettingsRepository,
    widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val _pendingAction = MutableStateFlow<AppAction?>(null)

    /**
     * Eingehende Aktion für Navigation und Steuerung (z.B. Shortcuts, Widgets, AppFunctions).
     * Wird nach erfolgreicher Verarbeitung über [consumeAction] auf null zurückgesetzt.
     */
    val pendingAction: StateFlow<AppAction?> = _pendingAction.asStateFlow()

    /**
     * Übergibt eine geparste Aktion (aus [MainActivity.onCreate] oder [MainActivity.onNewIntent])
     * zur asynchronen, sicheren Verarbeitung an die Compose-Navigationsebene.
     *
     * @param action Die zu verarbeitende [AppAction].
     */
    fun handleAction(action: AppAction?) {
        if (action != null) {
            _pendingAction.value = action
        }
    }

    /**
     * Quittiert die Verarbeitung der aktuellen Aktion und setzt den State zurück,
     * um eine doppelte Verarbeitung bei Recompositions oder Recreations zu verhindern.
     */
    fun consumeAction() {
        _pendingAction.value = null
    }

    /**
     * Reaktiver App-Settings-Flow für das globale Theme (themeMode, themeAmoled,
     * themeAccent). [SharingStarted.Eagerly] stellt sicher, dass die
     * Settings sofort beim App-Start verfügbar sind, ohne auf den ersten Collector
     * warten zu müssen – verhindert ein kurzes Theme-Flackern beim Kaltstart.
     */
    val appSettings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    /**
     * Exposes whether onboarding is completed for splash screen handling
     * and initial navigation key selection.
     */
    val onboardingCompleted: StateFlow<Boolean?> = appSettings
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
            } catch (e: Exception) {
                // Safeguard: Scheduler-Fehler dürfen den App-Start nicht blockieren.
                Log.w(TAG, "Fehler bei der Synchronisierung der Benachrichtigungsplanung beim App-Start", e)
            }
        }
        try {
            widgetUpdater.scheduleDailyUpdate()
        } catch (e: Exception) {
            // Safeguard: Fehler beim Widget-Scheduling dürfen den App-Start nicht blockieren.
            Log.w(TAG, "Fehler beim Planen der täglichen Widget-Aktualisierung beim App-Start", e)
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
