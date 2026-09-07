package com.heckmannch.birthdaybuddy.domain.repository

import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing application-wide settings and user preferences.
 */
interface SettingsRepository {

    /**
     * Reactive stream of current application configuration settings.
     */
    val settings: Flow<AppSettings>

    /**
     * Updates application settings by applying the given transformation function [transform]
     * to the current settings state.
     *
     * @param transform Function to apply modifications to the current [AppSettings].
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)

    /**
     * Retrieves a one-time snapshot of current application settings directly from storage.
     *
     * @return The current [AppSettings] snapshot.
     */
    suspend fun getSettingsImmediate(): AppSettings
}
