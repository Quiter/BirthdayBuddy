package com.heckmannch.birthdaybuddy.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Sucht rekursiv nach einer Activity im Context.
 * Hilfreich, da Contexts in Compose oft durch Wrapper (z.B. Hilt) umschlossen sind.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Öffnet sicher die Systemeinstellungen für die Anwendungsdetails dieser App.
 */
fun Context.openAppSettings() {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            if (this@openAppSettings !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
