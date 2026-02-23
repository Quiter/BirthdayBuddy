package com.heckmannch.birthdaybuddy.data

import androidx.room.TypeConverter
import com.heckmannch.birthdaybuddy.model.ContactActions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Konverter-Klasse für Room.
 * 
 * SQLite kann keine komplexen Objekte wie Listen oder eigene Datenklassen speichern.
 * Diese Klasse wandelt diese Objekte in JSON-Strings um, bevor sie in die Datenbank 
 * geschrieben werden, und parst sie beim Lesen wieder zurück.
 */
class DataConverters {
    /**
     * Wandelt eine Liste von Strings (Labels) in einen JSON-String um.
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    /**
     * Wandelt einen JSON-String zurück in eine Liste von Strings.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)

    /**
     * Wandelt die ContactActions (Messenger-Status etc.) in JSON um.
     */
    @TypeConverter
    fun fromContactActions(value: ContactActions): String = Json.encodeToString(value)

    /**
     * Wandelt JSON zurück in ein ContactActions-Objekt.
     */
    @TypeConverter
    fun toContactActions(value: String): ContactActions = Json.decodeFromString(value)
}
