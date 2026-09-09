package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import dagger.Reusable
import javax.inject.Inject

/**
 * Schedules the daily widget update alarm at midnight by delegating to
 * [WidgetUpdater.scheduleDailyUpdate].
 */
@Reusable
class ScheduleDailyWidgetUpdateUseCase @Inject constructor(
    private val widgetUpdater: WidgetUpdater,
) {
    operator fun invoke() {
        widgetUpdater.scheduleDailyUpdate()
    }
}
