package com.example.taskmanagerapp.data.mapper

import com.example.taskmanagerapp.data.local.TaskEntity
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.model.TaskPriority
import com.example.taskmanagerapp.domain.model.TaskStatus

fun TaskEntity.toDomain(): Task {
    val priorityEnum = try { TaskPriority.valueOf(priority) } catch (_: Exception) { TaskPriority.MEDIUM }
    val statusEnum = try { TaskStatus.valueOf(status) } catch (_: Exception) { TaskStatus.NOT_STARTED }

    return Task(
        id = id,
        title = title,
        description = description,
        completionTimeMillis = completionTimeMillis,
        creationTimeMillis = creationTimeMillis, // TaskEntity has non-null Long
        priority = priorityEnum,
        status = statusEnum,
        reminderDaysBefore = reminderDaysBefore,
        orderIndex = orderIndex
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        completionTimeMillis = completionTimeMillis,
        creationTimeMillis = creationTimeMillis,
        priority = priority.name,
        status = status.name,
        reminderDaysBefore = reminderDaysBefore,
        orderIndex = orderIndex
    )
}
