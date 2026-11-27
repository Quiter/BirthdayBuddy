package com.heckmannch.birthdaybuddy.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp
import com.heckmannch.birthdaybuddy.domain.util.PhoneNumberNormalizer

/**
 * Single source of truth for constructing messenger and SMS [Intent] instances across the UI
 * (`ContactActions`) and platform integrations (`BirthdayAppFunctionService`).
 *
 * Uses [Uri.Builder] for all query parameters so reserved characters such as `+` in E.164 phone
 * numbers are properly percent-encoded (`%2B`) instead of being interpreted as spaces.
 */
object MessengerIntentBuilder {

    /**
     * Supported WhatsApp package names in priority order:
     * standard WhatsApp (`com.whatsapp`), followed by WhatsApp Business (`com.whatsapp.w4b`).
     */
    val WHATSAPP_PACKAGES: List<String> = listOf(
        MessengerApp.WHATSAPP.packageName,
        "com.whatsapp.w4b",
    )

    /**
     * Resolves the installed WhatsApp package in priority order (`com.whatsapp`, then
     * `com.whatsapp.w4b`), or returns `null` if neither is installed or [packageManager] is `null`.
     */
    fun resolveWhatsAppPackage(
        packageManager: PackageManager?,
        packageInstalledCheck: (PackageManager, String) -> Boolean = ::isPackageInstalled,
    ): String? {
        val pm = packageManager ?: return null
        return WHATSAPP_PACKAGES.firstOrNull { pkg -> packageInstalledCheck(pm, pkg) }
    }

    /**
     * Builds an [Intent.ACTION_SENDTO] intent targeting the default SMS app for [phoneNumber].
     *
     * Normalizes [phoneNumber] to E.164 using [countryIso], falling back to the trimmed raw input
     * if normalization yields an empty string.
     */
    fun buildSmsIntent(
        phoneNumber: String,
        countryIso: String,
    ): Intent {
        val normalized = PhoneNumberNormalizer.normalize(phoneNumber, countryIso)
            .ifEmpty { phoneNumber.trim() }
        return Intent(Intent.ACTION_SENDTO, "smsto:$normalized".toUri())
    }

    /**
     * Builds an [Intent.ACTION_VIEW] intent for WhatsApp using `https://api.whatsapp.com/send`
     * with a digits-only E.164 `phone` query parameter.
     *
     * When [targetPackage] is non-null (e.g. `com.whatsapp` or `com.whatsapp.w4b`), the intent is
     * explicitly scoped to that package. Otherwise no package is set so Android can resolve the
     * universal HTTPS link via App Links or a web browser.
     */
    fun buildWhatsAppIntent(
        phoneNumber: String,
        countryIso: String,
        targetPackage: String? = null,
    ): Intent {
        val digitsOnly = PhoneNumberNormalizer.normalizeToDigitsOnly(phoneNumber, countryIso)
        val uri = Uri.Builder()
            .scheme("https")
            .authority("api.whatsapp.com")
            .appendPath("send")
            .appendQueryParameter("phone", digitsOnly)
            .build()
        return Intent(Intent.ACTION_VIEW, uri).apply {
            targetPackage?.let { setPackage(it) }
        }
    }

    /**
     * Builds an [Intent.ACTION_VIEW] intent for Signal (`https://signal.me/#p/<e164>`) scoped to
     * [MessengerApp.SIGNAL]'s package.
     *
     * Uses `encodedFragment` with `+` allowed so the official `signal.me/#p/+...` fragment syntax
     * is preserved while still encoding any unexpected characters.
     */
    fun buildSignalIntent(
        phoneNumber: String,
        countryIso: String,
    ): Intent {
        val cleanNumberWithPlus = PhoneNumberNormalizer.normalize(phoneNumber, countryIso)
        val uri = Uri.Builder()
            .scheme("https")
            .authority("signal.me")
            .path("/")
            .encodedFragment("p/${Uri.encode(cleanNumberWithPlus, "+")}")
            .build()
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(MessengerApp.SIGNAL.packageName)
        }
    }

    /**
     * Builds an [Intent.ACTION_VIEW] intent for Telegram (`tg://resolve?phone=%2B...`) with
     * properly URL-encoded query parameters.
     */
    fun buildTelegramIntent(
        phoneNumber: String,
        countryIso: String,
    ): Intent {
        val cleanNumberWithPlus = PhoneNumberNormalizer.normalize(phoneNumber, countryIso)
        val uri = Uri.Builder()
            .scheme("tg")
            .authority("resolve")
            .appendQueryParameter("phone", cleanNumberWithPlus)
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /**
     * Builds an [Intent.ACTION_VIEW] intent for Viber (`viber://keypad?number=%2B...`) with
     * properly URL-encoded query parameters.
     */
    fun buildViberIntent(
        phoneNumber: String,
        countryIso: String,
    ): Intent {
        val cleanNumberWithPlus = PhoneNumberNormalizer.normalize(phoneNumber, countryIso)
        val uri = Uri.Builder()
            .scheme("viber")
            .authority("keypad")
            .appendQueryParameter("number", cleanNumberWithPlus)
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /**
     * Builds an [Intent.ACTION_VIEW] intent for Skype (`skype:<e164>?chat`).
     */
    fun buildSkypeIntent(
        phoneNumber: String,
        countryIso: String,
    ): Intent {
        val cleanNumberWithPlus = PhoneNumberNormalizer.normalize(phoneNumber, countryIso)
        val uri = "skype:${Uri.encode(cleanNumberWithPlus, "+")}?chat".toUri()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    /**
     * Builds an [Intent] for the requested [MessengerApp] addressed to [phoneNumber], or returns
     * `null` for launch-only messengers if no launch intent is available from [packageManager].
     *
     * [whatsAppPackageResolver] is evaluated lazily only when [app] is [MessengerApp.WHATSAPP] to
     * avoid unnecessary [PackageManager] IPC calls for other messengers.
     */
    fun buildMessengerIntent(
        app: MessengerApp,
        phoneNumber: String,
        countryIso: String,
        packageManager: PackageManager? = null,
        whatsAppPackageResolver: () -> String? = { resolveWhatsAppPackage(packageManager) },
    ): Intent? = when (app) {
        MessengerApp.WHATSAPP -> buildWhatsAppIntent(
            phoneNumber = phoneNumber,
            countryIso = countryIso,
            targetPackage = whatsAppPackageResolver(),
        )

        MessengerApp.SIGNAL -> buildSignalIntent(
            phoneNumber = phoneNumber,
            countryIso = countryIso,
        )

        MessengerApp.TELEGRAM -> buildTelegramIntent(
            phoneNumber = phoneNumber,
            countryIso = countryIso,
        )

        MessengerApp.SKYPE -> buildSkypeIntent(
            phoneNumber = phoneNumber,
            countryIso = countryIso,
        )

        MessengerApp.VIBER -> buildViberIntent(
            phoneNumber = phoneNumber,
            countryIso = countryIso,
        )

        MessengerApp.THREEMA,
        MessengerApp.GOOGLE_MEET,
        MessengerApp.MESSENGER,
        MessengerApp.DISCORD,
        -> packageManager?.getLaunchIntentForPackage(app.packageName)
    }
}
