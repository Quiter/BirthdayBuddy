package com.heckmannch.birthdaybuddy.ui.components

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reagiert auf Änderungen im System-Adressbuch und ruft [onSyncNeeded] mit einem
 * Debounce von 1 Sekunde auf. Registriert und deregistriert den [ContentObserver]
 * automatisch mit dem Compose-Lifecycle.
 *
 * Erfordert die Berechtigung [android.Manifest.permission.READ_CONTACTS].
 * Fehlt die Berechtigung, wird die Registrierung stillschweigend ignoriert.
 */
@Composable
fun ContactSyncEffect(onSyncNeeded: () -> Unit) {
    val context = LocalContext.current
    // onSyncNeeded in eine stabile Referenz einwickeln, damit DisposableEffect
    // nicht bei jeder Rekomposition neu ausgelöst wird.
    val stableCallback = remember { onSyncNeeded }

    DisposableEffect(context) {
        val contentResolver = context.contentResolver
        val mainHandler = Handler(Looper.getMainLooper())
        val syncRunnable = Runnable { stableCallback() }
        val observer = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                mainHandler.removeCallbacks(syncRunnable)
                mainHandler.postDelayed(syncRunnable, 1000) // 1 Sekunde Debounce
            }
        }

        try {
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
        } catch (_: SecurityException) {
            // Keine Berechtigung vorhanden — kein Observer registriert
        }

        onDispose {
            contentResolver.unregisterContentObserver(observer)
            mainHandler.removeCallbacksAndMessages(null)
        }
    }
}
