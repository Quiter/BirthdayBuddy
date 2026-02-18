package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Holt den versionName der App dynamisch aus dem PackageManager.
 * Liefert einen Fallback-Wert, falls ein Fehler auftritt.
 */
@Composable
fun getAppVersionName(context: Context = LocalContext.current): String {
    return remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.6"
        } catch (e: Exception) {
            "0.6"
        }
    }
}
