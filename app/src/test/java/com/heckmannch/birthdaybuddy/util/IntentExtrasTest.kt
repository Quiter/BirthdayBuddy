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

    // =========================================================================
    // safeGetAndRemoveBooleanExtra Tests
    // =========================================================================

    @Test
    fun `safeGetAndRemoveBooleanExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        val result = IntentExtras.safeGetAndRemoveBooleanExtra(
            nullIntent,
            IntentExtras.NAVIGATE_TO_NOTIFICATIONS
        )

        assertThat(result).isFalse()

        val customDefaultResult = IntentExtras.safeGetAndRemoveBooleanExtra(
            nullIntent,
            IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
            defaultValue = true
        )
        assertThat(customDefaultResult).isTrue()
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.SCROLL_TO_TOP) } returns false

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.SCROLL_TO_TOP)

        assertThat(result).isFalse()
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra extracts boolean value and removes extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns true
        every { intent.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) } returns true
        every { intent.removeExtra(IntentExtras.OPEN_SEARCH) } just runs

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.OPEN_SEARCH)

        assertThat(result).isTrue()
        verify(exactly = 1) { intent.removeExtra(IntentExtras.OPEN_SEARCH) }
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra handles type mismatch gracefully without crashing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns true
        every {
            intent.getBooleanExtra(
                IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
                false
            )
        } throws ClassCastException("String cannot be cast to Boolean")
        every { intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } just runs

        val result = intent.safeGetAndRemoveBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)

        assertThat(result).isFalse()
        verify { intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) }
    }

    @Test
    fun `safeGetAndRemoveBooleanExtra handles cleanup exception when removeExtra throws in catch block`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns true
        every {
            intent.getBooleanExtra(
                IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
                false
            )
        } throws RuntimeException("Extraction error")
        every { intent.removeExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } throws RuntimeException("Cleanup error")

        val result = IntentExtras.safeGetAndRemoveBooleanExtra(
            intent,
            IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
            defaultValue = false
        )

        assertThat(result).isFalse()
    }

    // =========================================================================
    // safeGetAndRemoveStringExtra Tests
    // =========================================================================

    @Test
    fun `safeGetAndRemoveStringExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        val result =
            IntentExtras.safeGetAndRemoveStringExtra(nullIntent, IntentExtras.APPFN_CONTACT_ID)

        assertThat(result).isNull()

        val customDefault = IntentExtras.safeGetAndRemoveStringExtra(
            nullIntent,
            IntentExtras.APPFN_CONTACT_ID,
            "custom_default"
        )
        assertThat(customDefault).isEqualTo("custom_default")
    }

    @Test
    fun `safeGetAndRemoveStringExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns false

        val result = intent.safeGetAndRemoveStringExtra(IntentExtras.APPFN_CONTACT_ID)

        assertThat(result).isNull()
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetAndRemoveStringExtra extracts string value and removes extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } returns "contact_lookup_123"
        every { intent.removeExtra(IntentExtras.APPFN_CONTACT_ID) } just runs

        val result = intent.safeGetAndRemoveStringExtra(IntentExtras.APPFN_CONTACT_ID)

        assertThat(result).isEqualTo("contact_lookup_123")
        verify(exactly = 1) { intent.removeExtra(IntentExtras.APPFN_CONTACT_ID) }
    }

    @Test
    fun `safeGetAndRemoveStringExtra handles type mismatch gracefully without crashing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } throws ClassCastException("Int cannot be cast to String")
        every { intent.removeExtra(IntentExtras.APPFN_CONTACT_ID) } just runs

        val result = intent.safeGetAndRemoveStringExtra(IntentExtras.APPFN_CONTACT_ID, "default_id")

        assertThat(result).isEqualTo("default_id")
        verify { intent.removeExtra(IntentExtras.APPFN_CONTACT_ID) }
    }

    @Test
    fun `safeGetAndRemoveStringExtra handles cleanup exception when removeExtra throws in catch block`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } throws RuntimeException("Extraction error")
        every { intent.removeExtra(IntentExtras.APPFN_CONTACT_ID) } throws RuntimeException("Cleanup error")

        val result = IntentExtras.safeGetAndRemoveStringExtra(
            intent,
            IntentExtras.APPFN_CONTACT_ID,
            "fallback"
        )

        assertThat(result).isEqualTo("fallback")
    }

    // =========================================================================
    // safeGetAndRemoveIntExtra Tests
    // =========================================================================

    @Test
    fun `safeGetAndRemoveIntExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        val result =
            IntentExtras.safeGetAndRemoveIntExtra(nullIntent, IntentExtras.APPFN_BIRTHDAY_MONTH, -1)

        assertThat(result).isEqualTo(-1)

        val customDefault =
            IntentExtras.safeGetAndRemoveIntExtra(nullIntent, IntentExtras.APPFN_BIRTHDAY_MONTH, 5)
        assertThat(customDefault).isEqualTo(5)
    }

    @Test
    fun `safeGetAndRemoveIntExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns false

        val result = intent.safeGetAndRemoveIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1)

        assertThat(result).isEqualTo(-1)
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetAndRemoveIntExtra extracts int value and removes extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1) } returns 12
        every { intent.removeExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } just runs

        val result = intent.safeGetAndRemoveIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1)

        assertThat(result).isEqualTo(12)
        verify(exactly = 1) { intent.removeExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) }
    }

    @Test
    fun `safeGetAndRemoveIntExtra handles type mismatch gracefully without crashing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns true
        every {
            intent.getIntExtra(
                IntentExtras.APPFN_BIRTHDAY_MONTH,
                -1
            )
        } throws ClassCastException("String cannot be cast to Int")
        every { intent.removeExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } just runs

        val result = intent.safeGetAndRemoveIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1)

        assertThat(result).isEqualTo(-1)
        verify { intent.removeExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) }
    }

    @Test
    fun `safeGetAndRemoveIntExtra handles cleanup exception when removeExtra throws in catch block`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns true
        every {
            intent.getIntExtra(
                IntentExtras.APPFN_BIRTHDAY_MONTH,
                -1
            )
        } throws RuntimeException("Extraction error")
        every { intent.removeExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } throws RuntimeException("Cleanup error")

        val result = IntentExtras.safeGetAndRemoveIntExtra(
            intent,
            IntentExtras.APPFN_BIRTHDAY_MONTH,
            99
        )

        assertThat(result).isEqualTo(99)
    }

    // =========================================================================
    // safeGetBooleanExtra Tests
    // =========================================================================

    @Test
    fun `safeGetBooleanExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        assertThat(IntentExtras.safeGetBooleanExtra(nullIntent, "KEY", false)).isFalse()
        assertThat(nullIntent.safeGetBooleanExtra("KEY", true)).isTrue()
    }

    @Test
    fun `safeGetBooleanExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra("KEY_BOOLEAN") } returns false

        val result = IntentExtras.safeGetBooleanExtra(intent, "KEY_BOOLEAN", true)
        assertThat(result).isTrue()

        val extensionResult = intent.safeGetBooleanExtra("KEY_BOOLEAN", false)
        assertThat(extensionResult).isFalse()

        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetBooleanExtra extracts boolean value without removing extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns true
        every { intent.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) } returns true

        val result = intent.safeGetBooleanExtra(IntentExtras.OPEN_SEARCH)

        assertThat(result).isTrue()
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetBooleanExtra handles type mismatch gracefully`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns true
        every {
            intent.getBooleanExtra(
                IntentExtras.NAVIGATE_TO_NOTIFICATIONS,
                false
            )
        } throws ClassCastException()

        val result = intent.safeGetBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS)

        assertThat(result).isFalse()
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    // =========================================================================
    // safeGetStringExtra Tests
    // =========================================================================

    @Test
    fun `safeGetStringExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        assertThat(IntentExtras.safeGetStringExtra(nullIntent, "KEY")).isNull()
        assertThat(nullIntent.safeGetStringExtra("KEY", "fallback")).isEqualTo("fallback")
    }

    @Test
    fun `safeGetStringExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra("KEY_STR") } returns false

        val result = IntentExtras.safeGetStringExtra(intent, "KEY_STR", "def")
        assertThat(result).isEqualTo("def")

        val extensionResult = intent.safeGetStringExtra("KEY_STR")
        assertThat(extensionResult).isNull()

        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetStringExtra extracts string value without removing extra from intent`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } returns "contact_456"

        val result = intent.safeGetStringExtra(IntentExtras.APPFN_CONTACT_ID)

        assertThat(result).isEqualTo("contact_456")
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetStringExtra handles type mismatch gracefully`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } throws ClassCastException()

        val result = intent.safeGetStringExtra(IntentExtras.APPFN_CONTACT_ID, "fallback")

        assertThat(result).isEqualTo("fallback")
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    // =========================================================================
    // safeGetIntExtra Tests
    // =========================================================================

    @Test
    fun `safeGetIntExtra returns default value when intent is null`() {
        val nullIntent: Intent? = null
        assertThat(IntentExtras.safeGetIntExtra(nullIntent, "KEY")).isEqualTo(-1)
        assertThat(nullIntent.safeGetIntExtra("KEY", 42)).isEqualTo(42)
    }

    @Test
    fun `safeGetIntExtra returns default value when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra("KEY_INT") } returns false

        val result = IntentExtras.safeGetIntExtra(intent, "KEY_INT", -1)
        assertThat(result).isEqualTo(-1)

        val extensionResult = intent.safeGetIntExtra("KEY_INT", 100)
        assertThat(extensionResult).isEqualTo(100)

        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetIntExtra extracts valid int and handles type mismatch safely`() {
        val validIntent = mockk<Intent>()
        every { validIntent.hasExtra("KEY_INT") } returns true
        every { validIntent.getIntExtra("KEY_INT", -1) } returns 42

        val result = IntentExtras.safeGetIntExtra(validIntent, "KEY_INT", -1)
        assertThat(result).isEqualTo(42)
        verify(exactly = 0) { validIntent.removeExtra(any<String>()) }

        val invalidIntent = mockk<Intent>()
        every { invalidIntent.hasExtra("KEY_INT") } returns true
        every { invalidIntent.getIntExtra("KEY_INT", -1) } throws ClassCastException()

        val invalidResult = IntentExtras.safeGetIntExtra(invalidIntent, "KEY_INT", -1)
        assertThat(invalidResult).isEqualTo(-1)
        verify(exactly = 0) { invalidIntent.removeExtra(any<String>()) }
    }

    // =========================================================================
    // safeGetStringArrayExtra Tests
    // =========================================================================

    @Test
    fun `safeGetStringArrayExtra returns empty array when intent is null`() {
        val nullIntent: Intent? = null
        val result = IntentExtras.safeGetStringArrayExtra(nullIntent, "KEY_ARRAY")
        assertThat(result).isEmpty()
    }

    @Test
    fun `safeGetStringArrayExtra returns empty array when extra is missing`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra("KEY_STRINGS") } returns false

        val result = IntentExtras.safeGetStringArrayExtra(intent, "KEY_STRINGS")
        assertThat(result).isEmpty()
        verify(exactly = 0) { intent.removeExtra(any<String>()) }
    }

    @Test
    fun `safeGetStringArrayExtra extracts valid array and handles mismatch safely`() {
        val validIntent = mockk<Intent>()
        every { validIntent.hasExtra("KEY_STRINGS") } returns true
        every { validIntent.getStringArrayExtra("KEY_STRINGS") } returns arrayOf("a", "b")

        val result = IntentExtras.safeGetStringArrayExtra(validIntent, "KEY_STRINGS")
        assertThat(result).isEqualTo(arrayOf("a", "b"))
        verify(exactly = 0) { validIntent.removeExtra(any<String>()) }

        val invalidIntent = mockk<Intent>()
        every { invalidIntent.hasExtra("KEY_STRINGS") } returns true
        every { invalidIntent.getStringArrayExtra("KEY_STRINGS") } throws ClassCastException()

        val invalidResult = IntentExtras.safeGetStringArrayExtra(invalidIntent, "KEY_STRINGS")
        assertThat(invalidResult).isEmpty()
        verify(exactly = 0) { invalidIntent.removeExtra(any<String>()) }
    }
}


