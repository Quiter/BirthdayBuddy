package com.heckmannch.birthdaybuddy.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Android implementation of [PermissionChecker] using the application context
 * to check permission statuses against Android APIs.
 *
 * Design: Decouples the UI and domain layers from platform-specific APIs. Note that this implementation
 * class does NOT have `@Singleton` annotation on it directly (as per guideline 254), because it is
 * provided with `@Singleton` scope in the Hilt bindings module.
 *
 * @property context The application context injected via Hilt.
 */
class AndroidPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PermissionChecker {

    override fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun hasCalendarPermission(): Boolean {
        val hasReadCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val hasWriteCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        return hasReadCalendar && hasWriteCalendar
    }
}
