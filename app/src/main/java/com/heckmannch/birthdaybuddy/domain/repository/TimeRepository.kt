package com.heckmannch.birthdaybuddy.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain repository interface for date and time related queries (e.g. current date).
 */
interface TimeRepository {
    val currentDate: Flow<LocalDate>
}
