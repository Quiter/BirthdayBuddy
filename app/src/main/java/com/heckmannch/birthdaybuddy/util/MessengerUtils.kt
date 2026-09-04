package com.heckmannch.birthdaybuddy.util

import android.content.Context
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.MessengerApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchrone, nicht-blockierende Variante von [MessengerApp.getInstalledMessengers], die
 * die Binder-IPC-Aufrufe an den [android.content.pm.PackageManager] auf den IO-Dispatcher auslagert.
 */
suspend fun MessengerApp.Companion.getInstalledMessengersAsync(
    context: Context,
    forceRefresh: Boolean = false,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): List<MessengerApp> = withContext(ioDispatcher) {
    getInstalledMessengers(context, forceRefresh)
}
