package com.heckmannch.birthdaybuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import com.heckmannch.birthdaybuddy.data.repository.AuthRepository
import com.heckmannch.birthdaybuddy.data.repository.BirthdayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val filterManager: FilterManager,
    val authRepository: AuthRepository,
    private val birthdayRepository: BirthdayRepository
) : ViewModel() {
    val themeFlow = filterManager.preferencesFlow.map { it.theme }
    val currentUser = authRepository.currentUser

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).onSuccess {
                // Nach erfolgreichem Login: Geschenkideen synchronisieren
                birthdayRepository.syncGiftsFromCloud()
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
