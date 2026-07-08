package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper to convert between [AppSettingsEntity] and [AppSettings].
 */
@Reusable
class AppSettingsMapper @Inject constructor() {

    fun toDomain(entity: AppSettingsEntity): AppSettings {
        return AppSettings(
            id = entity.id,
            notificationsEnabled = entity.notificationsEnabled,
            persistentNotifications = entity.persistentNotifications,
            onboardingCompleted = entity.onboardingCompleted,
            lastSyncTimestamp = entity.lastSyncTimestamp,
            calendarSyncEnabled = entity.calendarSyncEnabled,
            calendarId = entity.calendarId,
            otherEventsEnabled = entity.otherEventsEnabled,
            ignoredCouplePairs = entity.ignoredCouplePairs,
            birthdayCalendarColor = entity.birthdayCalendarColor,
            anniversaryCalendarColor = entity.anniversaryCalendarColor,
            nameDayCalendarColor = entity.nameDayCalendarColor,
            themeMode = entity.themeMode,
            themeAmoled = entity.themeAmoled,
            themeAccent = entity.themeAccent,
            labelsEnabled = entity.labelsEnabled
        )
    }
}
