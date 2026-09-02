package com.heckmannch.birthdaybuddy.ui.components

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Reagiert auf Änderungen im System-Adressbuch und ruft [onSyncNeeded] mit einem
 * Debounce von 1 Sekunde auf. Registriert und deregistriert den [ContentObserver]
 * automatisch mit dem Compose-Lifecycle.
 *
 * Erfordert die Berechtigung [android.Manifest.permission.READ_CONTACTS].
 * Fehlt die Berechtigung, wird die Registrierung stillschweigend ignoriert.
 *
 * Architektur-Hinweis:
 * Für eine noch sauberere Entkopplung von der UI-Ebene kann dieser Observer künftig
 * in den Repository-Layer (z. B. `ContactRepository`) migriert werden. Dort kann er
 * über einen Coroutine-[kotlinx.coroutines.flow.callbackFlow] implementiert werden, der
 * System-Events bei Änderungen emittiert, mit `debounce(1000L)` entprellt und die
 * Registrierung bzw. Deregistrierung im `awaitClose`-Block lecksicher verwaltet.
 */
@Composable
fun ContactSyncEffect(onSyncNeeded: () -> Unit) {
    val context = LocalContext.current
    // onSyncNeeded in eine stabile Referenz einwickeln, damit DisposableEffect
    // nicht bei jeder Rekomposition neu ausgelöst wird.
    val currentCallback by rememberUpdatedState(onSyncNeeded)

    DisposableEffect(context) {
        val contentResolver = context.contentResolver
        val mainHandler = Handler(Looper.getMainLooper())
        val syncRunnable = Runnable { currentCallback() }
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
