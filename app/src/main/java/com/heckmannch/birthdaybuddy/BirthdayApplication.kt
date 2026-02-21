package com.heckmannch.birthdaybuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.heckmannch.birthdaybuddy.data.AppContainer
import com.heckmannch.birthdaybuddy.data.AppDataContainer

/**
 * Die Application-Klasse hält den Dependency Container bereit
 * und initialisiert zentrale System-Komponenten.
 */
class BirthdayApplication : Application() {

    /**
     * AppContainer-Instanz, die von anderen Klassen zur Dependency Injection genutzt wird.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        createNotificationChannel()
    }

    /**
     * Erstellt den Notification Channel einmalig beim App-Start.
     * Android ignoriert den Aufruf, falls der Channel bereits existiert.
     */
    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_name)
        val descriptionText = getString(R.string.notification_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("birthday_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
