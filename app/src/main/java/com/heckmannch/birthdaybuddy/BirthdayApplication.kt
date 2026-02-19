package com.heckmannch.birthdaybuddy

import android.app.Application
import com.heckmannch.birthdaybuddy.data.AppContainer
import com.heckmannch.birthdaybuddy.data.AppDataContainer

/**
 * Die Application-Klasse hält den Dependency Container bereit.
 */
class BirthdayApplication : Application() {

    /**
     * AppContainer-Instanz, die von anderen Klassen zur Dependency Injection genutzt wird.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
