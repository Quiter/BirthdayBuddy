package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import com.heckmannch.birthdaybuddy.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unterstützte Messenger-Apps für Direktaktionen aus der Geburtstagskarte.
 *
 * Beinhaltet Metadaten wie Paketname, Markenfarbe und Ressourcen-IDs für Icons und Labels.
 */
enum class MessengerApp(
    val packageName: String,
    val brandColor: Color,
    val labelResId: Int,
    val iconResId: Int
) {
    WHATSAPP(
        packageName = "com.whatsapp",
        brandColor = Color(0xFF25D366),
        labelResId = R.string.item_action_whatsapp,
        iconResId = R.drawable.ic_whatsapp
    ),
    SIGNAL(
        packageName = "org.thoughtcrime.securesms",
        brandColor = Color(0xFF3A76F0),
        labelResId = R.string.item_action_signal,
        iconResId = R.drawable.ic_signal
    ),
    THREEMA(
        packageName = "ch.threema.app",
        brandColor = Color(0xFF05A65D),
        labelResId = R.string.item_action_threema,
        iconResId = R.drawable.ic_threema
    ),
    MESSENGER(
        packageName = "com.facebook.orca",
        brandColor = Color(0xFF0084FF),
        labelResId = R.string.item_action_messenger,
        iconResId = R.drawable.ic_messenger
    ),
    TELEGRAM(
        packageName = "org.telegram.messenger",
        brandColor = Color(0xFF26A5E4),
        labelResId = R.string.item_action_telegram,
        iconResId = R.drawable.ic_telegram
    ),
    VIBER(
        packageName = "com.viber.voip",
        brandColor = Color(0xFF7360F2),
        labelResId = R.string.item_action_viber,
        iconResId = R.drawable.ic_viber
    ),
    GOOGLE_MEET(
        packageName = "com.google.android.apps.tachyon",
        brandColor = Color(0xFFFFEB3B),
        labelResId = R.string.item_action_google_meet,
        iconResId = R.drawable.ic_google_meet
    ),
    SKYPE(
        packageName = "com.skype.raider",
        brandColor = Color(0xFF00AFF0),
        labelResId = R.string.item_action_skype,
        iconResId = R.drawable.ic_skype
    ),
    DISCORD(
        packageName = "com.discord",
        brandColor = Color(0xFF5865F2),
        labelResId = R.string.item_action_discord,
        iconResId = R.drawable.ic_discord
    );

    companion object {
        @Volatile
        private var cachedInstalled: List<MessengerApp>? = null

        /**
         * Gibt die aktuell gecachte Liste der installierten Messenger-Apps zurück (sofern vorhanden),
         * ohne eine synchrone Abfrage des [PackageManager] auszuführen.
         */
        fun getCachedMessengers(): List<MessengerApp>? = cachedInstalled

        /**
         * Leert den Cache der installierten Messenger-Apps.
         */
        fun clearCache() {
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
        fun getInstalledMessengers(context: Context, forceRefresh: Boolean = false): List<MessengerApp> {
            if (forceRefresh) {
                clearCache()
            }
            return cachedInstalled ?: synchronized(this) {
                cachedInstalled ?: run {
                    val pm = context.packageManager
                    entries.filter { app ->
                        isPackageInstalled(pm, app.packageName)
                    }.also { cachedInstalled = it }
                }
            }
        }

        /**
         * Asynchrone, nicht-blockierende Variante von [getInstalledMessengers], die
         * die Binder-IPC-Aufrufe an den [PackageManager] auf den IO-Dispatcher auslagert.
         */
        suspend fun getInstalledMessengersAsync(
            context: Context,
            forceRefresh: Boolean = false,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
            } catch (_: Exception) {
                false
            }
        }
    }
}
