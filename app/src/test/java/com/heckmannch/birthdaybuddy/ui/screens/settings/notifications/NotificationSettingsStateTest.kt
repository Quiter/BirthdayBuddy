package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import org.junit.Test

class NotificationSettingsStateTest {

    private val dummyScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    @Test
    fun `saver preserves showAddDialog state`() {
        val original = NotificationSettingsState()
        original.openAddDialog()
        assertThat(original.showAddDialog).isTrue()
        assertThat(original.ruleToEdit).isNull()

        @Suppress("UNCHECKED_CAST")
        val saver = NotificationSettingsState.Saver as androidx.compose.runtime.saveable.Saver<NotificationSettingsState, Any>
        val saved = with(saver) {
            dummyScope.save(original)
        }
        assertThat(saved).isNotNull()

        val restored = saver.restore(saved!!)
        assertThat(restored).isNotNull()
        assertThat(restored!!.showAddDialog).isTrue()
        assertThat(restored.ruleToEdit).isNull()
    }

    @Test
    fun `saver preserves ruleToEdit state`() {
        val original = NotificationSettingsState()
        val rule = NotificationRule(id = 12, daysBefore = 3, hour = 18, minute = 45)
        original.openEditDialog(rule)
        assertThat(original.showAddDialog).isFalse()
        assertThat(original.ruleToEdit).isEqualTo(rule)

        @Suppress("UNCHECKED_CAST")
        val saver = NotificationSettingsState.Saver as androidx.compose.runtime.saveable.Saver<NotificationSettingsState, Any>
        val saved = with(saver) {
            dummyScope.save(original)
        }
        assertThat(saved).isNotNull()

        val restored = saver.restore(saved!!)
        assertThat(restored).isNotNull()
        assertThat(restored!!.showAddDialog).isFalse()
        assertThat(restored.ruleToEdit).isEqualTo(rule)
    }

    @Test
    fun `saver preserves default initial state`() {
        val original = NotificationSettingsState()
        assertThat(original.showAddDialog).isFalse()
        assertThat(original.ruleToEdit).isNull()

        @Suppress("UNCHECKED_CAST")
        val saver = NotificationSettingsState.Saver as androidx.compose.runtime.saveable.Saver<NotificationSettingsState, Any>
        val saved = with(saver) {
            dummyScope.save(original)
        }
        assertThat(saved).isNotNull()

        val restored = saver.restore(saved!!)
        assertThat(restored).isNotNull()
        assertThat(restored!!.showAddDialog).isFalse()
        assertThat(restored.ruleToEdit).isNull()
    }
}
