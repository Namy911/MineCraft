package com.example.minecraft.data.db.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class Converter {
    // Image Gallery resource
    @TypeConverter
    fun fromPreview(values: List<String> ): String {
        val gson = Gson()
        val type: Type = object : TypeToken<List<String?>?>() {}.type
        return gson.toJson(values, type)
    }

    @TypeConverter
    fun toPreview(value: String): List<String> {
        val gson = Gson()
        val type = object : TypeToken<List<String?>?>() {}.type
        return gson.fromJson(value, type)
    }
}