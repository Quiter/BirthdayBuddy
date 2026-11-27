package com.heckmannch.birthdaybuddy.ui.util

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp
import com.heckmannch.birthdaybuddy.domain.util.DeviceRegionProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [ContactActions] verifying normalized phone numbers, URI encoding,
 * package targeting, fallback behavior, and intent generation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContactActionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val testRegionProvider = object : DeviceRegionProvider {
        override fun getCountryIso(): String = "DE"
    }

    private val contactActions = ContactActions(
        context = context,
        deviceRegionProvider = testRegionProvider,
    )

    private fun installPackage(packageName: String) {
        val packageInfo = PackageInfo().apply { this.packageName = packageName }
        shadowOf(context.packageManager).installPackage(packageInfo)
    }

    @Test
    fun `dialNumber starts dial intent with normalized phone number`() {
        contactActions.dialNumber("0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(nextStartedActivity.dataString).isEqualTo("tel:+491701234567")
    }

    @Test
    fun `sendSms starts smsto intent with normalized phone number`() {
        contactActions.sendSms("0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(nextStartedActivity.dataString).isEqualTo("smsto:+491701234567")
    }

    @Test
    fun `openMessengerApp for WhatsApp starts view intent with digits-only phone number`() {
        contactActions.openMessengerApp(MessengerApp.WHATSAPP, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(nextStartedActivity.`package`).isNull()
    }

    @Test
    fun `openMessengerApp for WhatsApp targets standard package when installed`() {
        installPackage(MessengerApp.WHATSAPP.packageName)

        contactActions.openMessengerApp(MessengerApp.WHATSAPP, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(nextStartedActivity.`package`).isEqualTo(MessengerApp.WHATSAPP.packageName)
    }

    @Test
    fun `openMessengerApp for WhatsApp targets business package when only business is installed`() {
        installPackage("com.whatsapp.w4b")

        contactActions.openMessengerApp(MessengerApp.WHATSAPP, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(nextStartedActivity.`package`).isEqualTo("com.whatsapp.w4b")
    }

    @Test
    fun `openMessengerApp for WhatsApp falls back to package-less intent on ActivityNotFoundException`() {
        installPackage(MessengerApp.WHATSAPP.packageName)
        val startedIntents = mutableListOf<Intent>()
        val fallbackContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                startedIntents.add(Intent(intent))
                if (intent.`package` != null) {
                    throw ActivityNotFoundException("No handler for explicit WhatsApp package")
                }
            }
        }
        val actions = ContactActions(
            context = fallbackContext,
            deviceRegionProvider = testRegionProvider,
        )

        actions.openMessengerApp(MessengerApp.WHATSAPP, "0170 1234567")

        assertThat(startedIntents).hasSize(2)
        assertThat(startedIntents[0].`package`).isEqualTo(MessengerApp.WHATSAPP.packageName)
        assertThat(startedIntents[0].dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
        assertThat(startedIntents[1].`package`).isNull()
        assertThat(startedIntents[1].dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
    }

    @Test
    fun `openMessengerApp for Signal starts view intent with plus normalized phone number`() {
        contactActions.openMessengerApp(MessengerApp.SIGNAL, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://signal.me/#p/+491701234567")
        assertThat(nextStartedActivity.`package`).isEqualTo(MessengerApp.SIGNAL.packageName)
    }

    @Test
    fun `openMessengerApp for Telegram percent-encodes plus sign in phone query parameter`() {
        contactActions.openMessengerApp(MessengerApp.TELEGRAM, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("tg://resolve?phone=%2B491701234567")
        assertThat(nextStartedActivity.data?.getQueryParameter("phone")).isEqualTo("+491701234567")
    }

    @Test
    fun `openMessengerApp for Viber percent-encodes plus sign in number query parameter`() {
        contactActions.openMessengerApp(MessengerApp.VIBER, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("viber://keypad?number=%2B491701234567")
        assertThat(nextStartedActivity.data?.getQueryParameter("number")).isEqualTo("+491701234567")
    }

    @Test
    fun `openMessengerApp for Skype starts view intent with normalized phone number`() {
        contactActions.openMessengerApp(MessengerApp.SKYPE, "0170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("skype:+491701234567?chat")
    }

    @Test
    fun `convenience constructor with context resolves DeviceRegionProvider and works`() {
        val actionsFromContext = ContactActions(context)
        actionsFromContext.dialNumber("+49 170 1234567")

        val nextStartedActivity = shadowOf(context as Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(nextStartedActivity.dataString).isEqualTo("tel:+491701234567")
    }
}
