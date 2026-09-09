package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [ScheduleDailyWidgetUpdateUseCase].
 */
class ScheduleDailyWidgetUpdateUseCaseTest {

    private val widgetUpdater: WidgetUpdater = mock()
    private lateinit var useCase: ScheduleDailyWidgetUpdateUseCase

    @Before
    fun setUp() {
        useCase = ScheduleDailyWidgetUpdateUseCase(widgetUpdater)
    }

    @Test
    fun `when invoked, delegates to scheduleDailyUpdate on widgetUpdater`() = runTest {
        useCase()

        verify(widgetUpdater).scheduleDailyUpdate()
    }
}
