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
     * Öffnet WhatsApp mit einer Nachricht an die Nummer.
     */
    fun openWhatsApp(phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace("\\s+".toRegex(), "").replace("+", "")
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://api.whatsapp.com/send?phone=$cleanNumber".toUri()
            )
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /**
     * Öffnet Signal.
     */
    fun openSignal(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "https://signal.me/#p/$phoneNumber".toUri())
            context.startActivity(intent)
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
