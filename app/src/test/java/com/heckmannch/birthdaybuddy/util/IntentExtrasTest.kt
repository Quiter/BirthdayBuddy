package com.heckmannch.birthdaybuddy.util

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class IntentExtrasTest {

    @Test
    fun `safeGetAndRemoveBooleanExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        val result = IntentExtras.safeGetAndRemoveBooleanExtra(nullIntent, IntentExtras.NAVIGATE_TO_NOTIFICATIONS)

        assertThat(result).isFalse()
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.SCROLL_TO_TOP) } returns false

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.SCROLL_TO_TOP)

        assertThat(result).isFalse()
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra extracts boolean value and removes extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns true
        every { intent.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) } returns true
        every { intent.removeExtra(IntentExtras.OPEN_SEARCH) } just runs

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.OPEN_SEARCH)

        assertThat(result).isTrue()
        verify { intent.removeExtra(IntentExtras.OPEN_SEARCH) }
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra handles type mismatch gracefully without crashing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns true
        every { intent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false) } throws ClassCastException("String cannot be cast to Boolean")
        every { intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } just runs

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)

        assertThat(result).isFalse()
        verify { intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) }
    }

    @Test
    fun `safeGetIntExtra extracts valid int and handles type mismatch safely`() {
        val validIntent = mockk<Intent>()
        every { validIntent.hasExtra("KEY_INT") } returns true
        every { validIntent.getIntExtra("KEY_INT", -1) } returns 42

        val result = IntentExtras.safeGetIntExtra(validIntent, "KEY_INT", -1)
        assertThat(result).isEqualTo(42)

        val invalidIntent = mockk<Intent>()
        every { invalidIntent.hasExtra("KEY_INT") } returns true
        every { invalidIntent.getIntExtra("KEY_INT", -1) } throws ClassCastException()

        val invalidResult = IntentExtras.safeGetIntExtra(invalidIntent, "KEY_INT", -1)
        assertThat(invalidResult).isEqualTo(-1)
    }

    @Test
    fun `safeGetStringArrayExtra extracts valid array and handles mismatch safely`() {
        val validIntent = mockk<Intent>()
        every { validIntent.hasExtra("KEY_STRINGS") } returns true
        every { validIntent.getStringArrayExtra("KEY_STRINGS") } returns arrayOf("a", "b")

        val result = IntentExtras.safeGetStringArrayExtra(validIntent, "KEY_STRINGS")
        assertThat(result).isEqualTo(arrayOf("a", "b"))

        val invalidIntent = mockk<Intent>()
        every { invalidIntent.hasExtra("KEY_STRINGS") } returns true
        every { invalidIntent.getStringArrayExtra("KEY_STRINGS") } throws ClassCastException()

        val invalidResult = IntentExtras.safeGetStringArrayExtra(invalidIntent, "KEY_STRINGS")
        assertThat(invalidResult).isEmpty()
    }
}
