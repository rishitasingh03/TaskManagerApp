package com.example.taskmanagerapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
//Middle layer between view model and room
class TaskRepository(private val dao: TaskDao) {
//converts task entity into task
    fun tasksFlow(): Flow<List<Task>> {
        return dao.getAllFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllOnce(): List<Task> {
        return dao.getAllOnce().map { it.toDomain() }
    }

    suspend fun insert(task: Task) {
        dao.insert(task.toEntity())
    }

    suspend fun insertAll(tasks: List<Task>) {
        dao.insertAll(tasks.map { it.toEntity() })
    }

    suspend fun update(task: Task) {
        dao.update(task.toEntity())
    }

    suspend fun delete(task: Task) {
        dao.delete(task.toEntity())
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun maxIdOrNull(): Long? = dao.maxIdOrNull()
}
