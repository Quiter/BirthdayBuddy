package com.heckmannch.birthdaybuddy.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.MessengerApp
import com.heckmannch.birthdaybuddy.util.findActivity

/**
 * Zentralisiert alle externen Aktionen (Intents) und Permission-Logik.
 * Sorgt für einheitliches Error-Handling und saubere Composables.
 */
class ContactActions(private val context: Context) {

    /**
     * Öffnet die Telefon-App mit der gewählten Nummer.
     */
    fun dialNumber(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet die SMS-App.
     */
    fun sendSms(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumber".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet einen Messenger mit der gewählten Nummer.
     */
    fun openMessengerApp(app: MessengerApp, phoneNumber: String) {
        try {
            val digitsOnly = phoneNumber.replace("\\s+".toRegex(), "").replace("+", "")
            val cleanNumberWithPlus = if (phoneNumber.startsWith("+")) {
                phoneNumber.replace("\\s+".toRegex(), "")
            } else {
                "+" + phoneNumber.replace("\\s+".toRegex(), "")
            }

            val intent = when (app) {
                MessengerApp.WHATSAPP -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://api.whatsapp.com/send?phone=$digitsOnly".toUri()
                    )
                }

                MessengerApp.SIGNAL -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        "signal://conversation?number=$cleanNumberWithPlus".toUri()
                    ).apply {
                        setPackage(MessengerApp.SIGNAL.packageName)
                    }
                }

                MessengerApp.TELEGRAM -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        "tg://resolve?phone=$cleanNumberWithPlus".toUri()
                    )
                }

                MessengerApp.SKYPE -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        "skype:$cleanNumberWithPlus?chat".toUri()
                    )
                }

                MessengerApp.VIBER -> {
                    Intent(
                        Intent.ACTION_VIEW,
                        "viber://keypad?number=$cleanNumberWithPlus".toUri()
                    )
                }

                MessengerApp.THREEMA,
                MessengerApp.GOOGLE_MEET,
                MessengerApp.MESSENGER,
                MessengerApp.DISCORD -> {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                }
            }

            intent?.let {
                context.startActivity(it)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet einen Kontakt in der Android Kontakte-App.
     */
    fun openContact(id: String, lookupKey: String) {
        try {
            id.toLongOrNull()?.let { numericId ->
                val lookupUri = ContactsContract.Contacts.getLookupUri(numericId, lookupKey)
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet den "Kontakt hinzufügen" Dialog des Systems.
     */
    fun addContact() {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet die App-Einstellungen (für Berechtigungen).
     */
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Regelt die Permission-Abfrage inkl. Rationale-Handling.
     */
    fun requestContactPermission(
        launcher: ActivityResultLauncher<String>,
        hasAttemptedBefore: Boolean,
        onSetAttempted: () -> Unit
    ) {
        val activity = context.findActivity()
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.READ_CONTACTS
            )
        } ?: false

        if (shouldShowRationale || !hasAttemptedBefore) {
            launcher.launch(Manifest.permission.READ_CONTACTS)
            onSetAttempted()
        } else {
            openAppSettings()
        }
    }
}
