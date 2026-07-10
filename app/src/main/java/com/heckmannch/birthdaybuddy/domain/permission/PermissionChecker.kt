package com.heckmannch.birthdaybuddy.domain.permission

/**
 * Interface for checking Android system permissions.
 *
 * Design: Decouples ViewModels and other domain logic from direct Android platform API calls
 * (such as `ContextCompat.checkSelfPermission`), improving testability and adhering to
 * Clean Architecture principles.
 */
interface PermissionChecker {
    /**
     * Checks if the app has permission to read contacts.
     *
     * @return true if the contacts permission is granted, false otherwise.
     */
    fun hasContactsPermission(): Boolean

    /**
     * Checks if the app has permission to post notifications.
     *
     * @return true if notification permission is granted, false otherwise.
     */
    fun hasNotificationPermission(): Boolean

    /**
     * Checks if the app has permission to read and write calendars.
     *
     * @return true if calendar permissions are granted, false otherwise.
     */
    fun hasCalendarPermission(): Boolean
}
