package com.heckmannch.birthdaybuddy2.viewmodel

import androidx.compose.runtime.Immutable

/**
 * UI-Modell für einen Kontakt. 
 */
@Immutable
data class ContactUiModel(
    val id: String, // Interner Key oder lookupKey
    val contactId: String,
    val lookupKey: String,
    val fullName: String,
    val dateText: String,
    val monthName: String,
    val imageUri: String?,
    val initials: String,
    val nextAge: Int?,
    val nextAgeText: String?,
    val daysUntilNext: Long,
    val daysLeftText: String,
    val isToday: Boolean,
    val giftIdeas: String?,
)

@Immutable
data class LabelManagementModel(
    val name: String,
    val isHiddenFromFilter: Boolean,
    val isIgnored: Boolean,
    val isSystem: Boolean,
)

@Immutable
data class GiftIdea(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
) {
    companion object {
        fun fromString(encoded: String?): List<GiftIdea> {
            if (encoded.isNullOrBlank()) return emptyList()
            return encoded.split(";;").mapNotNull {
                val parts = it.split("|", limit = 2)
                if (parts.size == 2) {
                    GiftIdea(text = parts[1], isChecked = parts[0] == "1")
                } else null
            }
        }

        fun toString(ideas: List<GiftIdea>): String {
            return ideas.joinToString(";;") { "${if (it.isChecked) "1" else "0"}|${it.text}" }
        }
    }
}
