package com.example.taskmanagerapp

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoomConverters {
    private val gson = Gson()

    //convert list into json string
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    //convert json string into list of int
    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        return if (data.isNullOrEmpty()) emptyList() else gson.fromJson(data, object : TypeToken<List<Int>>() {}.type)
    }

    //enum into string
    @TypeConverter
    fun fromPriority(p: TaskPriority?): String? = p?.name

    @TypeConverter
    fun toPriority(value: String?): TaskPriority =
        value?.let { TaskPriority.valueOf(it) } ?: TaskPriority.MEDIUM

    @TypeConverter
    fun fromStatus(s: TaskStatus?): String? = s?.name

    @TypeConverter
    fun toStatus(value: String?): TaskStatus =
        value?.let { TaskStatus.valueOf(it) } ?: TaskStatus.NOT_STARTED
}
