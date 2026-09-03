package com.example.rickandmorty.core.database

import androidx.room.TypeConverter

/**
 * The API's relations (a character's episodes, an episode's cast, a location's residents)
 * are id lists. They are only ever read whole, so a comma-joined column is enough and
 * avoids pulling a serializer into the database layer.
 */
class Converters {

    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(separator = ",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(',').mapNotNull(String::toIntOrNull)
}
