package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import androidx.compose.ui.graphics.Color
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.MessengerApp

/**
 * UI-Metadaten und Ressourcen-Zuordnungen für [MessengerApp].
 *
 * Kapselt Markenfarben sowie Ressourcen-IDs für Labels und Icons im UI-Layer,
 * um das Domain-Modell frei von UI- und Framework-Abhängigkeiten zu halten.
 */
val MessengerApp.brandColor: Color
    get() = when (this) {
        MessengerApp.WHATSAPP -> Color(0xFF25D366)
        MessengerApp.SIGNAL -> Color(0xFF3A76F0)
        MessengerApp.THREEMA -> Color(0xFF05A65D)
        MessengerApp.MESSENGER -> Color(0xFF0084FF)
        MessengerApp.TELEGRAM -> Color(0xFF26A5E4)
        MessengerApp.VIBER -> Color(0xFF7360F2)
        MessengerApp.GOOGLE_MEET -> Color(0xFFFFEB3B)
        MessengerApp.SKYPE -> Color(0xFF00AFF0)
        MessengerApp.DISCORD -> Color(0xFF5865F2)
    }

val MessengerApp.labelResId: Int
    get() = when (this) {
        MessengerApp.WHATSAPP -> R.string.item_action_whatsapp
        MessengerApp.SIGNAL -> R.string.item_action_signal
        MessengerApp.THREEMA -> R.string.item_action_threema
        MessengerApp.MESSENGER -> R.string.item_action_messenger
        MessengerApp.TELEGRAM -> R.string.item_action_telegram
        MessengerApp.VIBER -> R.string.item_action_viber
        MessengerApp.GOOGLE_MEET -> R.string.item_action_google_meet
        MessengerApp.SKYPE -> R.string.item_action_skype
        MessengerApp.DISCORD -> R.string.item_action_discord
    }

val MessengerApp.iconResId: Int
    get() = when (this) {
        MessengerApp.WHATSAPP -> R.drawable.ic_whatsapp
        MessengerApp.SIGNAL -> R.drawable.ic_signal
        MessengerApp.THREEMA -> R.drawable.ic_threema
        MessengerApp.MESSENGER -> R.drawable.ic_messenger
        MessengerApp.TELEGRAM -> R.drawable.ic_telegram
        MessengerApp.VIBER -> R.drawable.ic_viber
        MessengerApp.GOOGLE_MEET -> R.drawable.ic_google_meet
        MessengerApp.SKYPE -> R.drawable.ic_skype
        MessengerApp.DISCORD -> R.drawable.ic_discord
    }
