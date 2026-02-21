package com.heckmannch.birthdaybuddy.utils

import android.content.Context
import com.heckmannch.birthdaybuddy.R
import kotlin.random.Random

/**
 * Generiert personalisierte und lokalisierte Geburtstagswünsche aus den Ressourcen.
 */
object GreetingGenerator {

    /**
     * Erzeugt eine zufällige, passende Nachricht basierend auf Alter und Name.
     * Es wird erwartet, dass das Alter als erster Format-Parameter (%1$d) und der Name als zweiter (%2$s) verwendet wird.
     */
    fun generateRandomGreeting(context: Context, name: String, age: Int): String {
        val isRound = age > 0 && age % 10 == 0
        
        val arrayId = when {
            isRound -> R.array.greetings_round
            age < 13 -> R.array.greetings_kid
            age in 13..19 -> R.array.greetings_teen
            else -> R.array.greetings_adult
        }

        return try {
            val greetings = context.resources.getStringArray(arrayId)
            val template = greetings.random()
            // String.format ignoriert überflüssige Argumente. Wichtig ist, dass die Indizes (%1$d, %2$s) in den XMLs stimmen.
            String.format(template, age, name)
        } catch (e: Exception) {
            // Sicherer Fallback, falls mit den Ressourcen oder Platzhaltern etwas nicht stimmt.
            context.getString(R.string.share_subject) + " $name! 🎉"
        }
    }
}
