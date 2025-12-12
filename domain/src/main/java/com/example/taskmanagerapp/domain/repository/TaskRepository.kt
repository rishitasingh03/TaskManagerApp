package com.example.taskmanagerapp.domain.repository

import com.example.taskmanagerapp.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun tasksFlow(): Flow<List<Task>>
    suspend fun getAllOnce(): List<Task>
    suspend fun insert(task: Task): Long
    suspend fun insertAll(tasks: List<Task>)
    suspend fun update(task: Task)
    suspend fun delete(task: Task)
    suspend fun deleteById(id: Long)
    suspend fun maxIdOrNull(): Long?
}
