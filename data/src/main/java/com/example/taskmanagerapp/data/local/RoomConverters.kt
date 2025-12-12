package com.example.taskmanagerapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoomConverters {
    private val gson = Gson()

    //convert list of int into json string
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? = list?.let { gson.toJson(it) }
    //convert json string into list of int
    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }
}
