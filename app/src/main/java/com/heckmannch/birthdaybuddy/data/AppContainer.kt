package com.heckmannch.birthdaybuddy.data

import android.content.Context
import com.heckmannch.birthdaybuddy.utils.NotificationHelper

/**
 * Dependency Injection Container auf App-Ebene.
 */
interface AppContainer {
    val birthdayRepository: BirthdayRepository
    val filterManager: FilterManager
    val notificationHelper: NotificationHelper
}

/**
 * Implementierung des Containers, die die Instanzen verwaltet.
 */
class AppDataContainer(private val context: Context) : AppContainer {

    override val birthdayRepository: BirthdayRepository by lazy {
        BirthdayRepository(context)
    }

    override val filterManager: FilterManager by lazy {
        FilterManager(context)
    }

    override val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(context)
    }
}
