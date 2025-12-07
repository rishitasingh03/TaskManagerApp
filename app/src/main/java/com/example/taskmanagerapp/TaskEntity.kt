package com.example.taskmanagerapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val description: String,
    val completionTimeMillis: Long,
    val creationTimeMillis: Long?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val reminderDaysBefore: List<Int>,
    val orderIndex: Int
)

// Mapping helpers (convert between domain Task and TaskEntity)
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    completionTimeMillis = completionTimeMillis,
    creationTimeMillis = creationTimeMillis ?: 0L,
    priority = priority,
    status = status,
    reminderDaysBefore = reminderDaysBefore,
    orderIndex = orderIndex
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    completionTimeMillis = completionTimeMillis,
    creationTimeMillis = creationTimeMillis,
    priority = priority,
    status = status,
    reminderDaysBefore = reminderDaysBefore,
    orderIndex = orderIndex
)
