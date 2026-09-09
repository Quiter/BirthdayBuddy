package com.heckmannch.birthdaybuddy.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val TAG = "MessengerUtils"

@Volatile
private var cachedInstalled: List<MessengerApp>? = null

/**
 * Gibt die aktuell gecachte Liste der installierten Messenger-Apps zurück (sofern vorhanden),
 * ohne eine synchrone Abfrage des [PackageManager] auszuführen.
 */
fun MessengerApp.Companion.getCachedMessengers(): List<MessengerApp>? = cachedInstalled

/**
 * Leert den Cache der installierten Messenger-Apps.
 */
fun MessengerApp.Companion.clearCache() {
    synchronized(this) {
        cachedInstalled = null
    }
}

/**
 * Prüft, welche Messenger-Apps auf dem Gerät installiert sind, unter Verwendung
 * eines thread-sicheren In-Memory-Caches.
 *
 * @param context Der Kontext für den Zugriff auf den [PackageManager].
 * @param forceRefresh Erzwingt eine erneute Prüfung und Invalidierung des Caches.
 * @return Eine Liste der auf dem Gerät installierten [MessengerApp]-Einträge.
 */
fun MessengerApp.Companion.getInstalledMessengers(
    context: Context,
    forceRefresh: Boolean = false,
): List<MessengerApp> {
    if (forceRefresh) {
        clearCache()
    }
    return cachedInstalled ?: synchronized(this) {
        cachedInstalled ?: run {
            val pm = context.packageManager
            MessengerApp.entries.filter { app ->
                isPackageInstalled(pm, app.packageName)
            }.also { cachedInstalled = it }
        }
    }
}

/**
 * Asynchrone, nicht-blockierende Variante von [MessengerApp.getInstalledMessengers], die
 * die Binder-IPC-Aufrufe an den [android.content.pm.PackageManager] auf den IO-Dispatcher auslagert.
 */
suspend fun MessengerApp.Companion.getInstalledMessengersAsync(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
    forceRefresh: Boolean = false,
): List<MessengerApp> = withContext(ioDispatcher) {
    getInstalledMessengers(context, forceRefresh)
}

/**
 * Prüft abwärtskompatibel, ob ein Paket auf dem Gerät installiert ist.
 * Verwendet ab API 33 (Tiramisu) die typsichere [PackageManager.PackageInfoFlags]-API.
 */
private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (e: Exception) {
        Log.w(TAG, "Unerwarteter Fehler beim Prüfen der Installation von Paket: $packageName", e)
        false
    }
}
