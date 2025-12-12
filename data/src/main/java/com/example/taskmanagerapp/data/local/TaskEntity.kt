package com.example.taskmanagerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val completionTimeMillis: Long,
    val creationTimeMillis: Long = 0L,
    val priority: String,
    val status: String,
    val reminderDaysBefore: List<Int> = emptyList(),
    val orderIndex: Int = 0
)
