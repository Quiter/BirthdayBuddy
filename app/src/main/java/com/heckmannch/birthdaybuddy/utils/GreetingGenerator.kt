package com.heckmannch.birthdaybuddy.utils

import kotlin.random.Random

/**
 * Generiert personalisierte und altersgerechte Geburtstagswünsche.
 */
object GreetingGenerator {

    private val kidGreetings = listOf(
        "Alles Gute zum %d. Geburtstag, kleiner Champion! 🎉 Lass dich heute reich beschenken und feier eine tolle Party! 🎂",
        "Happy Birthday, %s! ✨ %d Jahre alt - jetzt bist du schon richtig groß! Hab einen super Tag mit viel Kuchen! 🎈",
        "Hurra, du hast Geburtstag! 🥳 Alles Liebe zu deinem %d. Ehrentag. Viel Spaß beim Spielen und Feiern! 🎁"
    )

    private val teenGreetings = listOf(
        "Happy Birthday, %s! 🎂 %d Jahre - genieß die Zeit und lass dich heute ordentlich feiern! 🤘✨",
        "Alles Gute zum %d.! 🎉 Ich wünsch dir einen mega Tag mit deinen Freunden und alles Gute für das neue Lebensjahr! 🚀",
        "Glückwunsch zum Geburtstag! 🥳 %d Jahre alt zu sein ist echt cool. Hab einen entspannten Tag! 🎈"
    )

    private val adultGreetings = listOf(
        "Herzlichen Glückwunsch zum %d. Geburtstag, %s! 🎉 Ich wünsche dir einen wundervollen Tag im Kreise deiner Liebsten. 🎂✨",
        "Alles Liebe zum Geburtstag! 🥂 %d Jahre sind ein tolles Alter. Ich wünsche dir viel Gesundheit und Freude für dein neues Lebensjahr! 🎈",
        "Happy Birthday! 🥳 Lass dich heute verwöhnen und genieß deinen Ehrentag in vollen Zügen. Alles Gute! 🎁✨",
        "Ich wünsche dir alles erdenklich Gute zu deinem %d. Geburtstag! 🎂 Auf ein weiteres Jahr voller schöner Momente! 🥂"
    )

    private val roundGreetings = listOf(
        "Wow, %d Jahre! 🎉 Herzlichen Glückwunsch zu diesem tollen Meilenstein, %s! Lass es heute richtig krachen! 🥂🎂",
        "Happy Birthday zur runden %d! 🥳 Ein neues Jahrzehnt voller Möglichkeiten beginnt. Alles Liebe und viel Erfolg! ✨🚀",
        "Alles Gute zum %d. Geburtstag! 🎈 So ein besonderes Jubiläum muss gebührend gefeiert werden. Hab einen unvergesslichen Tag! 🥂🎁"
    )

    /**
     * Erzeugt eine zufällige, passende Nachricht.
     */
    fun generateRandomGreeting(name: String, age: Int): String {
        val isRound = age > 0 && age % 10 == 0
        
        val list = when {
            isRound -> roundGreetings
            age < 13 -> kidGreetings
            age in 13..19 -> teenGreetings
            else -> adultGreetings
        }

        val template = list[Random.nextInt(list.size)]
        
        // Template befüllen (manche brauchen Namen, manche nicht)
        return try {
            if (template.contains("%s") && template.contains("%d")) {
                template.format(name, age)
            } else if (template.contains("%s")) {
                template.format(name)
            } else {
                template.format(age)
            }
        } catch (e: Exception) {
            "Alles Liebe zum $age. Geburtstag, $name! 🎉"
        }
    }
}
