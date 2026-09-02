package com.heckmannch.birthdaybuddy.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Sucht rekursiv nach einer Activity im Context.
 * Hilfreich, da Contexts in Compose oft durch Wrapper (z.B. Hilt) umschlossen sind.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Öffnet sicher die Systemeinstellungen für die Anwendungsdetails dieser App.
 */
fun Context.openAppSettings() {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
    }
}
