package com.example.taskmanagerapp

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.taskmanagerapp.di.AppModule
// this file is Used to create TaskViewModel instances
//Ensures ViewModel is created the same way as production

class TaskViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(
                application = application,
                repository = AppModule.provideTaskRepository(application),
                enableDbSync = true
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
