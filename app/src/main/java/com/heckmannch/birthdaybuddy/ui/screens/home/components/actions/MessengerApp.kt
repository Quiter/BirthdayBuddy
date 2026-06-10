package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.heckmannch.birthdaybuddy.R

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
        private var cachedInstalled: List<MessengerApp>? = null

        fun getInstalledMessengers(context: Context): List<MessengerApp> {
            return cachedInstalled ?: synchronized(this) {
                cachedInstalled ?: run {
                    val pm = context.packageManager
                    entries.filter { app ->
                        try {
                            pm.getPackageInfo(app.packageName, 0)
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }.also { cachedInstalled = it }
                }
            }
        }
    }
}
