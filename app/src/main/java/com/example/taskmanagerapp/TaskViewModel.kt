package com.example.taskmanagerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private var nextId = 1L //counter for id (long)

    // Backing state for the screen
    private var uIState by mutableStateOf(TaskUiState())
    val uiState: TaskUiState get() = uIState //read-only version of the state

    private val prefsName = "task_manager_prefs"
    private val tasksKey = "tasks_json"
    private val nextIdKey = "next_id"

    private val gson = Gson()
    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    init {
        loadFromPrefs()
    }

    private fun persistTasks() {
        try {
            val tasksJson = gson.toJson(uIState.tasks)
            prefs.edit()
                .putString(tasksKey, tasksJson)
                .putLong(nextIdKey, nextId)
                .apply()
        } catch (e: Exception) {
            // ignore or log if you want
        }
    }

    private fun loadFromPrefs() {
        try {
            val tasksJson = prefs.getString(tasksKey, null)
            if (!tasksJson.isNullOrEmpty()) {
                val listType = object : TypeToken<List<Task>>() {}.type
                val loaded: List<Task> = gson.fromJson(tasksJson, listType)
                uIState = uIState.copy(tasks = loaded)
            }
            // restore nextId, default 1 if missing
            nextId = prefs.getLong(nextIdKey, 1L)
            // ensure nextId higher than any existing id
            val maxId = uIState.tasks.maxOfOrNull { it.id } ?: 0L
            if (nextId <= maxId) nextId = maxId + 1
        } catch (e: Exception) {
            // ignore or log if you want
        }
    }


    //Called when user clicks on Add Task button.
    fun startAddTask() {
        val now = System.currentTimeMillis()
        uIState = uIState.copy(
            isFormVisible = true,
            formState = TaskFormState(
                taskIdBeingEdited = null,
                title = "",
                description = "",
                completionDate = "",
                completionTime = "",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.NOT_STARTED,
                reminderDay1 = false,
                reminderDay2 = false,
                reminderDay3 = false,
                creationTimeMillis = null,
                isEditable = true,
                errorMessage = null
            )
        )
    }


     // Called when user clicks on a task in the list to edit it.

    fun startEditTask(task: Task) {
        //completionTimeMillis is splitted into completion date and time
        val (dateString, timeString) = splitDateAndTime(task.completionTimeMillis)
        val now = System.currentTimeMillis()
        val editable = now < task.completionTimeMillis

        uIState = uIState.copy(
            isFormVisible = true,
            formState = TaskFormState(
                taskIdBeingEdited = task.id,
                title = task.title,
                description = task.description,
                completionDate = dateString,
                completionTime = timeString,
                priority = task.priority,
                status = task.status,
                reminderDay1 = task.reminderDaysBefore.contains(1),
                reminderDay2 = task.reminderDaysBefore.contains(2),
                reminderDay3 = task.reminderDaysBefore.contains(3),
                creationTimeMillis = task.creationTimeMillis,
                isEditable = editable,
                errorMessage = if (!editable) {
                    "Task can no longer be edited because completion time has passed."
                } else null
            )
        )
    }

//Feild Updaters

    fun updateTitle(value: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(title = value, errorMessage = null)
        )
    }

    fun updateDescription(value: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(description = value, errorMessage = null)
        )
    }

    fun updateCompletionDate(value: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(completionDate = value, errorMessage = null)
        )
    }

    fun updateCompletionTime(value: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(completionTime = value, errorMessage = null)
        )
    }

    fun updatePriority(priority: TaskPriority) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(priority = priority, errorMessage = null)
        )
    }

    fun updateStatus(status: TaskStatus) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(status = status, errorMessage = null)
        )
    }

    fun updateReminderDay1(checked: Boolean) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(reminderDay1 = checked, errorMessage = null)
        )
    }

    fun updateReminderDay2(checked: Boolean) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(reminderDay2 = checked, errorMessage = null)
        )
    }

    fun updateReminderDay3(checked: Boolean) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(reminderDay3 = checked, errorMessage = null)
        )
    }

    //Form Actions

     // User cancels form (either Add or Edit).
    fun cancelEditing() {
        uIState = uIState.copy(
            isFormVisible = false, //hides form
            formState = TaskFormState() //reset
        )
    }


     // Save button clicked- validate inputs, then add/update task.
    fun saveTask() {
        val current = uIState.formState

        if (!current.isEditable) {
            uIState = uIState.copy(
                formState = current.copy(
                    errorMessage = "This task can no longer be edited."
                )
            )
            return
        }

        if (current.title.isBlank()) {
            setError("Title is required.")
            return
        }

        if (current.completionDate.isBlank() || current.completionTime.isBlank()) {
            setError("Completion date and time are required.")
            return
        }

        val completionMillis = parseToMillis(
            current.completionDate,
            current.completionTime
        )

        if (completionMillis == null) {
            setError("Date/time format is invalid. Use dd-MM-yyyy for date and HH:mm for time.")
            return
        }

        val now = System.currentTimeMillis()
        if (completionMillis <= now) {
            setError("Completion time should be in the future.")
            return
        }

        // Build reminder list from checkboxes (max 3)
        val reminderDays = mutableListOf<Int>()
        if (current.reminderDay1) reminderDays.add(1)
        if (current.reminderDay2) reminderDays.add(2)
        if (current.reminderDay3) reminderDays.add(3)

        val creationTime = current.creationTimeMillis ?: now

        if (current.taskIdBeingEdited == null) {
            // Add new task
            val newTask = Task(
                id = nextId++,
                title = current.title.trim(),
                description = current.description.trim(),
                completionTimeMillis = completionMillis,
                creationTimeMillis = creationTime,
                priority = current.priority,
                status = current.status,
                reminderDaysBefore = reminderDays
            )
            uIState = uIState.copy(
                tasks = uIState.tasks + newTask,
                isFormVisible = false,
                formState = TaskFormState()
            )
            persistTasks()
        } else {
            // Edit existing task
            val updatedTasks = uIState.tasks.map { existing ->
                if (existing.id == current.taskIdBeingEdited) {
                    existing.copy(
                        title = current.title.trim(),
                        description = current.description.trim(),
                        completionTimeMillis = completionMillis,
                        priority = current.priority,
                        status = current.status,
                        reminderDaysBefore = reminderDays
                    )
                } else {
                    existing
                }
            }
            uIState = uIState.copy(
                tasks = updatedTasks,
                isFormVisible = false,
                formState = TaskFormState()
            )
            persistTasks()
        }

        // schedule actual reminder
    }
     // Delete the task currently being edited.

    fun deleteCurrentTask() {
        val id = uIState.formState.taskIdBeingEdited ?: return
        val remaining = uIState.tasks.filterNot { it.id == id }//filters that task from list
        uIState = uIState.copy(
            tasks = remaining,
            isFormVisible = false, //hide form
            formState = TaskFormState()
        )
        persistTasks()
    }

    private fun setError(message: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(errorMessage = message)
        )
    }
}
