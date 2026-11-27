package com.heckmannch.birthdaybuddy.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [MessengerIntentBuilder] covering URI construction, percent-encoding of query
 * parameters, WhatsApp package resolution, and launch-only messenger fallbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MessengerIntentBuilderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun installPackage(packageName: String) {
        val packageInfo = PackageInfo().apply { this.packageName = packageName }
        shadowOf(context.packageManager).installPackage(packageInfo)
    }

    @Test
    fun `buildTelegramIntent percent-encodes plus sign in phone query parameter`() {
        val intent = MessengerIntentBuilder.buildTelegramIntent("0170 1234567", "DE")

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("tg://resolve?phone=%2B491701234567")
        assertThat(intent.data?.getQueryParameter("phone")).isEqualTo("+491701234567")
    }

    @Test
    fun `buildViberIntent percent-encodes plus sign in number query parameter`() {
        val intent = MessengerIntentBuilder.buildViberIntent("+49 (0) 170 1234567", "DE")

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("viber://keypad?number=%2B491701234567")
        assertThat(intent.data?.getQueryParameter("number")).isEqualTo("+491701234567")
    }

    @Test
    fun `buildWhatsAppIntent builds digits-only query parameter with optional package`() {
        val targeted = MessengerIntentBuilder.buildWhatsAppIntent(
            phoneNumber = "+49 170 1234567",
            countryIso = "DE",
            targetPackage = "com.whatsapp",
        )
        assertThat(targeted.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(targeted.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(targeted.data?.getQueryParameter("phone")).isEqualTo("491701234567")
        assertThat(targeted.`package`).isEqualTo("com.whatsapp")

        val universal = MessengerIntentBuilder.buildWhatsAppIntent(
            phoneNumber = "0170 1234567",
            countryIso = "DE",
            targetPackage = null,
        )
        assertThat(universal.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(universal.`package`).isNull()
    }

    @Test
    fun `buildSignalIntent preserves official fragment format and targets Signal package`() {
        val intent = MessengerIntentBuilder.buildSignalIntent("0170 1234567", "DE")

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("https://signal.me/#p/+491701234567")
        assertThat(intent.`package`).isEqualTo(MessengerApp.SIGNAL.packageName)
    }

    @Test
    fun `buildSkypeIntent builds skype chat uri`() {
        val intent = MessengerIntentBuilder.buildSkypeIntent("0170 1234567", "DE")

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("skype:+491701234567?chat")
    }

    @Test
    fun `buildSmsIntent normalizes number and falls back to trimmed raw input when empty`() {
        val normalized = MessengerIntentBuilder.buildSmsIntent("0170 1234567", "DE")
        assertThat(normalized.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(normalized.dataString).isEqualTo("smsto:+491701234567")

        val fallback = MessengerIntentBuilder.buildSmsIntent("  *100#  ", "DE")
        assertThat(fallback.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(fallback.dataString).isEqualTo("smsto:100")

        val nonDigitFallback = MessengerIntentBuilder.buildSmsIntent("  abc  ", "DE")
        assertThat(nonDigitFallback.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(nonDigitFallback.dataString).isEqualTo("smsto:abc")
    }

    @Test
    fun `resolveWhatsAppPackage prioritizes standard WhatsApp over Business and returns null when absent`() {
        assertThat(MessengerIntentBuilder.resolveWhatsAppPackage(null)).isNull()
        assertThat(MessengerIntentBuilder.resolveWhatsAppPackage(context.packageManager)).isNull()

        installPackage("com.whatsapp.w4b")
        assertThat(MessengerIntentBuilder.resolveWhatsAppPackage(context.packageManager))
            .isEqualTo("com.whatsapp.w4b")

        installPackage("com.whatsapp")
        assertThat(MessengerIntentBuilder.resolveWhatsAppPackage(context.packageManager))
            .isEqualTo("com.whatsapp")
    }

    @Test
    fun `buildMessengerIntent does not invoke whatsAppPackageResolver for non-WhatsApp messengers`() {
        var resolverInvoked = false
        val intent = MessengerIntentBuilder.buildMessengerIntent(
            app = MessengerApp.TELEGRAM,
            phoneNumber = "0170 1234567",
            countryIso = "DE",
            whatsAppPackageResolver = {
                resolverInvoked = true
                "com.whatsapp"
            },
        )

        assertThat(intent).isNotNull()
        assertThat(intent!!.dataString).isEqualTo("tg://resolve?phone=%2B491701234567")
        assertThat(resolverInvoked).isFalse()
    }

    @Test
    fun `buildMessengerIntent returns null for launch-only messengers when not installed`() {
        val launchOnlyApps = listOf(
            MessengerApp.THREEMA,
            MessengerApp.GOOGLE_MEET,
            MessengerApp.MESSENGER,
            MessengerApp.DISCORD,
        )

        launchOnlyApps.forEach { app ->
            val intent = MessengerIntentBuilder.buildMessengerIntent(
                app = app,
                phoneNumber = "0170 1234567",
                countryIso = "DE",
                packageManager = context.packageManager,
            )
            assertThat(intent).isNull()
        }
    }
}
