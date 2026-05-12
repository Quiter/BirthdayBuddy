package com.heckmannch.birthdaybuddy.database

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromString(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromList(list: List<String>?): String = list?.joinToString("|") ?: ""

    @TypeConverter
    fun toList(data: String?): List<String> = if (data.isNullOrBlank()) emptyList() else data.split("|")
}
