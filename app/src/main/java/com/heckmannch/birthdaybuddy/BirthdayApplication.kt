package com.heckmannch.birthdaybuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Die zentrale Application-Klasse der App.
 * 
 * Diese Klasse ist der Einstiegspunkt des Prozesses. Wir nutzen hier:
 * 1. @HiltAndroidApp: Initialisiert das Hilt-Dependency-Injection-Framework. Ohne dies
 *    könnten wir keine ViewModel-Injektion oder Repository-Bereitstellung nutzen.
 * 2. Configuration.Provider: Erlaubt es uns, den WorkManager für Hintergrundaufgaben 
 *    (wie die tägliche Geburtstagsprüfung) mit Hilt zu verknüpfen.
 */
@HiltAndroidApp
class BirthdayApplication : Application(), Configuration.Provider {

    // HiltWorkerFactory wird benötigt, damit der WorkManager Klassen mit 
    // @HiltWorker-Annotation (wie unseren BirthdayWorker) instanziieren kann.
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Konfiguriert den WorkManager so, dass er Hilt für die Erstellung von Workern nutzt.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Wir erstellen den Benachrichtigungskanal direkt beim Start, damit die App
        // jederzeit bereit ist, Erinnerungen zu senden.
        createNotificationChannel()
    }

    /**
     * Erstellt einen Notification Channel (erforderlich ab Android 8.0).
     * 
     * Channels erlauben es dem Nutzer, Benachrichtigungskategorien in den 
     * Systemeinstellungen individuell zu steuern (z.B. Ton aus, aber Banner an).
     */
    private fun createNotificationChannel() {
        val name = getString(R.string.notification_channel_name)
        val descriptionText = getString(R.string.notification_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH
        
        // Die ID "birthday_channel" muss konsistent mit der ID im NotificationHelper sein.
        val channel = NotificationChannel("birthday_channel", name, importance).apply {
            description = descriptionText
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
