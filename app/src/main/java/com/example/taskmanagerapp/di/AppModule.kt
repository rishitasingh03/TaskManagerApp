package com.example.taskmanagerapp.di

import android.content.Context
import com.example.taskmanagerapp.data.local.AppDatabase
import com.example.taskmanagerapp.data.repository.TaskRepositoryImpl
import com.example.taskmanagerapp.domain.repository.TaskRepository

object AppModule {
    fun provideTaskRepository(context: Context): TaskRepository {
        val dao = AppDatabase.getInstance(context).taskDao()
        return TaskRepositoryImpl(dao)
    }
}
