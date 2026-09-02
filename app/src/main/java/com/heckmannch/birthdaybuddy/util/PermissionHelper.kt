package com.heckmannch.birthdaybuddy.util

import android.app.Activity
import androidx.core.app.ActivityCompat

fun Activity.shouldShowRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}
