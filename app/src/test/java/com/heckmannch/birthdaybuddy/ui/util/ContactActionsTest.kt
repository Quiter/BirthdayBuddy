package com.heckmannch.birthdaybuddy.ui.util

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.util.DeviceRegionProvider
import com.heckmannch.birthdaybuddy.ui.screens.home.components.actions.MessengerApp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for [ContactActions] verifying normalized phone numbers and intent generation.
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

    @Test
    fun `dialNumber starts dial intent with normalized phone number`() {
        contactActions.dialNumber("0170 1234567")

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(nextStartedActivity.dataString).isEqualTo("tel:+491701234567")
    }

    @Test
    fun `sendSms starts smsto intent with normalized phone number`() {
        contactActions.sendSms("0170 1234567")

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(nextStartedActivity.dataString).isEqualTo("smsto:+491701234567")
    }

    @Test
    fun `openMessengerApp for WhatsApp starts view intent with digits-only phone number`() {
        contactActions.openMessengerApp(MessengerApp.WHATSAPP, "0170 1234567")

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://api.whatsapp.com/send?phone=491701234567")
    }

    @Test
    fun `openMessengerApp for Signal starts view intent with plus normalized phone number`() {
        contactActions.openMessengerApp(MessengerApp.SIGNAL, "0170 1234567")

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(nextStartedActivity.dataString).isEqualTo("https://signal.me/#p/+491701234567")
    }

    @Test
    fun `convenience constructor with context resolves DeviceRegionProvider and works`() {
        val actionsFromContext = ContactActions(context)
        actionsFromContext.dialNumber("+49 170 1234567")

        val nextStartedActivity = shadowOf(context as android.app.Application).nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(nextStartedActivity.dataString).isEqualTo("tel:+491701234567")
    }
}
