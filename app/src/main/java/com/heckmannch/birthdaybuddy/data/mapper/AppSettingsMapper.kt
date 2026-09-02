package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper to convert between [AppSettingsEntity] and [AppSettings].
 */
@Reusable
class AppSettingsMapper @Inject constructor() {

    fun toDomain(entity: AppSettingsEntity): AppSettings {
        val isHex = entity.themeAccent.startsWith("#")
        val themeAccentEnum = if (isHex) {
            ThemeAccent.CUSTOM
        } else {
            runCatching { ThemeAccent.valueOf(entity.themeAccent) }.getOrDefault(ThemeAccent.SYSTEM)
        }
        val customAccentColor = if (isHex) entity.themeAccent else null

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
            themeAccent = themeAccentEnum,
            customAccentColor = customAccentColor,
            labelsEnabled = entity.labelsEnabled
        )
    }

    fun toEntity(domain: AppSettings): AppSettingsEntity {
        val themeAccent = if (domain.themeAccent == ThemeAccent.CUSTOM && !domain.customAccentColor.isNullOrEmpty()) {
            domain.customAccentColor
        } else {
            domain.themeAccent.name
        }

        return AppSettingsEntity(
            id = domain.id,
            notificationsEnabled = domain.notificationsEnabled,
            persistentNotifications = domain.persistentNotifications,
            onboardingCompleted = domain.onboardingCompleted,
            lastSyncTimestamp = domain.lastSyncTimestamp,
            calendarSyncEnabled = domain.calendarSyncEnabled,
            calendarId = domain.calendarId,
            otherEventsEnabled = domain.otherEventsEnabled,
            ignoredCouplePairs = domain.ignoredCouplePairs,
            birthdayCalendarColor = domain.birthdayCalendarColor,
            anniversaryCalendarColor = domain.anniversaryCalendarColor,
            nameDayCalendarColor = domain.nameDayCalendarColor,
            themeMode = domain.themeMode,
            themeAmoled = domain.themeAmoled,
            themeAccent = themeAccent,
            labelsEnabled = domain.labelsEnabled
        )
    }
}
