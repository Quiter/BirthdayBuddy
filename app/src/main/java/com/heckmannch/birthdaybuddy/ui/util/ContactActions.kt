package com.heckmannch.birthdaybuddy.ui.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp
import com.heckmannch.birthdaybuddy.domain.util.DeviceRegionProvider
import com.heckmannch.birthdaybuddy.domain.util.PhoneNumberNormalizer
import com.heckmannch.birthdaybuddy.util.MessengerIntentBuilder
import com.heckmannch.birthdaybuddy.util.findActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Zentralisiert alle externen Aktionen (Intents) und Permission-Logik.
 * Sorgt für einheitliches Error-Handling und saubere Composables.
 */
class ContactActions @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceRegionProvider: DeviceRegionProvider,
) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ContactActionsEntryPoint {
        fun deviceRegionProvider(): DeviceRegionProvider
    }

    /**
     * Convenience secondary constructor for Composable call sites without direct Hilt constructor injection.
     * Resolves [DeviceRegionProvider] dynamically via [EntryPointAccessors] with a safe fallback for test runners.
     */
    constructor(context: Context) : this(
        context = context,
        deviceRegionProvider = runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ContactActionsEntryPoint::class.java
            ).deviceRegionProvider()
        }.getOrElse {
            object : DeviceRegionProvider {
                override fun getCountryIso(): String = "DE"
            }
        }
    )

    /**
     * Öffnet die Telefon-App mit der gewählten Nummer.
     */
    fun dialNumber(phoneNumber: String) {
        try {
            val normalized = PhoneNumberNormalizer.normalize(phoneNumber, deviceRegionProvider.getCountryIso())
                .ifEmpty { phoneNumber.trim() }
            val intent = Intent(Intent.ACTION_DIAL, "tel:$normalized".toUri())
            startActivitySafely(intent)
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
            Log.w(TAG, "Keine passende App zum Wählen der Rufnummer gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Starten des Wählvorgangs.", e)
        }
    }

    /**
     * Opens the default SMS application addressed to [phoneNumber].
     */
    fun sendSms(phoneNumber: String) {
        try {
            val intent = MessengerIntentBuilder.buildSmsIntent(
                phoneNumber = phoneNumber,
                countryIso = deviceRegionProvider.getCountryIso(),
            )
            startActivitySafely(intent)
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
            Log.w(TAG, "Keine passende SMS-App gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Senden der SMS.", e)
        }
    }

    /**
     * Opens the requested [MessengerApp] addressed to [phoneNumber].
     *
     * Delegates URI/intent creation to [MessengerIntentBuilder]. For [MessengerApp.WHATSAPP], if an
     * explicitly targeted WhatsApp package fails with [ActivityNotFoundException], retries once
     * without a package restriction so the universal HTTPS deep link can be resolved by the system.
     */
    fun openMessengerApp(app: MessengerApp, phoneNumber: String) {
        try {
            val intent = MessengerIntentBuilder.buildMessengerIntent(
                app = app,
                phoneNumber = phoneNumber,
                countryIso = deviceRegionProvider.getCountryIso(),
                packageManager = context.packageManager,
            )

            intent?.let { targetIntent ->
                try {
                    startActivitySafely(targetIntent)
                } catch (e: ActivityNotFoundException) {
                    if ((app == MessengerApp.WHATSAPP) && (targetIntent.`package` != null)) {
                        // Fallback to universal HTTPS link without explicit package restriction
                        startActivitySafely(Intent(targetIntent).setPackage(null))
                    } else {
                        throw e
                    }
                }
            }
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
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
                startActivitySafely(Intent(Intent.ACTION_VIEW, lookupUri))
            }
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
            Log.w(TAG, "Keine Kontakte-App zum Anzeigen des Kontakts gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Anzeigen des Kontakts.", e)
        }
    }

    /**
     * Öffnet einen Kontakt direkt im Bearbeitungsmodus der Android Kontakte-App.
     */
    fun editContact(id: String, lookupKey: String) {
        try {
            val numericId = id.toLongOrNull()
            val lookupUri = if (numericId != null) {
                ContactsContract.Contacts.getLookupUri(numericId, lookupKey)
                    ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, numericId)
            } else if (lookupKey.isNotBlank()) {
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, Uri.encode(lookupKey))
            } else {
                null
            }

            if (lookupUri != null) {
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(lookupUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
                try {
                    startActivitySafely(intent)
                } catch (_: ActivityNotFoundException) {
                    // Fallback ohne expliziten MIME-Type falls Hersteller-App setDataAndType nicht unterstützt
                    val fallbackIntent = Intent(Intent.ACTION_EDIT, lookupUri).apply {
                        putExtra("finishActivityOnSaveCompleted", true)
                    }
                    startActivitySafely(fallbackIntent)
                }
            } else {
                Log.w(TAG, "Kontakt-URI konnte für ID=$id und LookupKey=$lookupKey nicht erzeugt werden.")
            }
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
            Log.w(TAG, "Keine Kontakte-App zum Bearbeiten des Kontakts gefunden.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Bearbeiten des Kontakts.", e)
        }
    }

    /**
     * Öffnet den "Kontakt hinzufügen" Dialog des Systems.
     */
    fun addContact() {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                setDataAndType(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts.CONTENT_TYPE)
            }
            startActivitySafely(intent)
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
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
            startActivitySafely(intent)
        } catch (_: ActivityNotFoundException) {
            // Expected: no handler installed for this intent
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

    private fun startActivitySafely(intent: Intent) {
        if (context.findActivity() == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private const val TAG = "ContactActions"
    }
}
