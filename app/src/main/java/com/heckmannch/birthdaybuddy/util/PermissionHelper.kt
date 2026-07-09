package com.heckmannch.birthdaybuddy.util

import android.app.Activity
import androidx.core.app.ActivityCompat

class PermissionHelper(private val activity: Activity) {
    fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
