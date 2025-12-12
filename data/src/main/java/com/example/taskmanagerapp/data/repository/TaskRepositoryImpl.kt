package com.example.taskmanagerapp.data.repository

import com.example.taskmanagerapp.data.local.TaskDao
import com.example.taskmanagerapp.data.mapper.toDomain
import com.example.taskmanagerapp.data.mapper.toEntity
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//Middle layer between view model and room
class TaskRepositoryImpl(private val dao: TaskDao) : TaskRepository {
    //converts task entity into task
    override fun tasksFlow(): Flow<List<Task>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllOnce(): List<Task> =
        dao.getAll().map { it.toDomain() }

    override suspend fun insert(task: Task): Long =
        dao.insert(task.toEntity())

    override suspend fun insertAll(tasks: List<Task>) {
        dao.insertAll(tasks.map { it.toEntity() })
    }
    override suspend fun update(task: Task) {
        dao.update(task.toEntity())
    }

    override suspend fun delete(task: Task) {
        dao.delete(task.toEntity())
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun maxIdOrNull(): Long? =
        dao.maxIdOrNull()
}
