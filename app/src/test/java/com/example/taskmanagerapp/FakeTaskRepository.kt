package com.example.taskmanagerapp

import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
// this file is a test-only, in-memory implementation of TaskRepository

//Implements the same interface as the real repository
class FakeTaskRepository : TaskRepository {

    //Acts like a fake database table
    //Lives only in memory
    //Reset automatically for every test run
    //This guarantees test isolation
    private val taskList = mutableListOf<Task>()

    //Mimics Room’s Flow<List<Task>>
    //Emits current task list once
    override fun tasksFlow(): Flow<List<Task>> = flow {
        emit(taskList)
    }

    override suspend fun getAllOnce(): List<Task> = taskList

    //Simulates inserting a task
    //Returns task ID just like Room would
    override suspend fun insert(task: Task): Long {
        taskList.add(task)
        return task.id
    }

    //Used in tests where multiple tasks are needed
    override suspend fun insertAll(tasks: List<Task>) {
        taskList.addAll(tasks)
    }

    //Finds task by ID
    //Replaces it with updated version
    // Mirrors real update behavior without DB complexity.
    override suspend fun update(task: Task) {
        val index = taskList.indexOfFirst { it.id == task.id }
        if (index != -1) taskList[index] = task
    }

    //Used by ViewModel delete logic
    //Enables testing delete behavior safely
    override suspend fun delete(task: Task) {
        taskList.remove(task)
    }
    override suspend fun deleteById(id: Long) {
        taskList.removeIf { it.id == id }
    }

    //ViewModel may generate next task ID
    //Fake repository must support that logic
    override suspend fun maxIdOrNull(): Long? =
        taskList.maxOfOrNull { it.id }
}

//This file is an in-memory replacement for the real database,
// allowing fast, isolated, and reliable unit tests without Room.