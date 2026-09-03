package com.htj.habitzy.data.local.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
}
