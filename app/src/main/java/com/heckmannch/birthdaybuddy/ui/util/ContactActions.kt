package com.heckmannch.birthdaybuddy.ui.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.domain.util.PhoneNumberNormalizer
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
            val normalized = PhoneNumberNormalizer.normalize(phoneNumber).ifEmpty { phoneNumber.trim() }
            val intent = Intent(Intent.ACTION_DIAL, "tel:$normalized".toUri())
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Keine passende App zum Wählen der Rufnummer gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Starten des Wählvorgangs.", e)
        }
    }

    /**
     * Öffnet die SMS-App.
     */
    fun sendSms(phoneNumber: String) {
        try {
            val normalized = PhoneNumberNormalizer.normalize(phoneNumber).ifEmpty { phoneNumber.trim() }
            val intent = Intent(Intent.ACTION_SENDTO, "smsto:$normalized".toUri())
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Keine passende SMS-App gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Senden der SMS.", e)
        }
    }

    /**
     * Öffnet einen Messenger mit der gewählten Nummer.
     */
    fun openMessengerApp(app: MessengerApp, phoneNumber: String) {
        try {
            val cleanNumberWithPlus = PhoneNumberNormalizer.normalize(phoneNumber)
            val digitsOnly = PhoneNumberNormalizer.normalizeToDigitsOnly(phoneNumber)

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
                        "https://signal.me/#p/$cleanNumberWithPlus".toUri()
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
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Messenger-App (${app.name}) konnte nicht geöffnet werden: Keine passende App installiert.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Öffnen der Messenger-App (${app.name}).", e)
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
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Keine Kontakte-App zum Anzeigen des Kontakts gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Anzeigen des Kontakts.", e)
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
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Keine passende App zum Erstellen von Kontakten gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Öffnen des 'Kontakt hinzufügen'-Dialogs.", e)
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
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Keine Einstellungs-App gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Öffnen der App-Einstellungen.", e)
        }
    }

    /**
     * Regelt die Permission-Abfrage inkl. Rationale-Handling für multiple Permissions (READ_CONTACTS, WRITE_CONTACTS).
     */
    fun requestContactPermissions(
        launcher: ActivityResultLauncher<Array<String>>,
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
            launcher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                )
            )
            onSetAttempted()
        } else {
            openAppSettings()
        }
    }

    companion object {
        private const val TAG = "ContactActions"
    }
}
