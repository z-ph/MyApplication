package com.example.myapplication.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room type converters for complex types
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromMap(map: Map<String, Any>?): String {
        return gson.toJson(map ?: emptyMap<String, Any>())
    }

    @TypeConverter
    fun toMap(json: String?): Map<String, Any> {
        if (json == null) return emptyMap()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
