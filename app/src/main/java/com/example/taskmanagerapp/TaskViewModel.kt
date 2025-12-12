package com.example.taskmanagerapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.model.TaskFormState
import com.example.taskmanagerapp.domain.model.TaskPriority
import com.example.taskmanagerapp.domain.model.TaskStatus
import com.example.taskmanagerapp.domain.model.TaskUiState
import com.example.taskmanagerapp.domain.model.parseToMillis
import com.example.taskmanagerapp.domain.model.splitDateAndTime
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private var nextId = 1L //counter for id (long)

    // Backing state for the screen
    private var uIState by mutableStateOf(TaskUiState())
    val uiState: TaskUiState get() = uIState //read-only version of the state

    // Use the AppModule provider to get the concrete repository implementation
    private val repository: com.example.taskmanagerapp.domain.repository.TaskRepository =
        com.example.taskmanagerapp.di.AppModule.provideTaskRepository(getApplication())

    init {
        subscribeToDb() //keep ui in sync with room
    }

    private fun insertTaskAsync(task: Task) {
        viewModelScope.launch {
            try {
                repository.insert(task)
            } catch (t: Throwable) {
                t.printStackTrace()
                setGlobalError("Saving failed — please try again.")
            }
        }
    }

    private fun updateTaskAsync(task: Task) {
        viewModelScope.launch {
            try {
                repository.update(task)
            } catch (t: Throwable) {
                t.printStackTrace()
                setGlobalError("Saving failed — please try again.")
            }
        }
    }

    private fun deleteTaskAsync(task: Task) {
        viewModelScope.launch {
            try {
                repository.delete(task)
            } catch (t: Throwable) {
                t.printStackTrace()
                setGlobalError("Delete failed — please try again.")
            }
        }
    }

    private fun deleteByIdAsync(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteById(id)
            } catch (t: Throwable) {
                t.printStackTrace()
                setGlobalError("Delete failed — please try again.")
            }
        }
    }


    private fun subscribeToDb() {
        // collect tasks from repo and update UI state
        viewModelScope.launch {
            repository.tasksFlow().collect { domainTasks ->
                // ensure nextId remains unique: set nextId to max(existing ids)+1 if needed
                if (domainTasks.isNotEmpty()) {
                    val maxId = domainTasks.maxOf { it.id }
                    if (nextId <= maxId) nextId = maxId + 1
                }
                uIState = uIState.copy(tasks = domainTasks)
            }
        }
    }


     // global error message for the UI.
    private fun setGlobalError(message: String) {
        // store ERROR MESSAGE so UI can show it through existing form error display
        uIState = uIState.copy(formState = uIState.formState.copy(errorMessage = message))
    }

    //Clear the global error after UI consumes it
    fun clearError() {
        uIState = uIState.copy(formState = uIState.formState.copy(errorMessage = null))
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

//Field Updaters

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
                reminderDaysBefore = reminderDays,
                orderIndex = uIState.tasks.size
            )
            uIState = uIState.copy(
                tasks = uIState.tasks + newTask,
                isFormVisible = false,
                formState = TaskFormState()
            )
            insertTaskAsync(newTask)
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
            val editedId = current.taskIdBeingEdited
            val updatedTask = updatedTasks.firstOrNull { it.id == editedId }
            uIState = uIState.copy(
                tasks = updatedTasks,
                isFormVisible = false,
                formState = TaskFormState()
            )
            updatedTask?.let { updateTaskAsync(it) } //EVERY TIME CALLED TO PERSIST THE CHANGE

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
        deleteByIdAsync(id) //delete from database
    }

    private fun setError(message: String) {
        uIState = uIState.copy(
            formState = uIState.formState.copy(errorMessage = message)
        )
    }


     // Delete task at the given list index. Returns the removed Task or null if invalid index.

    fun deleteTaskAt(index: Int): Task? {
        return try {
            val tasks = uIState.tasks
            if (index < 0 || index >= tasks.size) return null
            val mutable = tasks.toMutableList()
            val removed = mutable.removeAt(index)
            uIState = uIState.copy(tasks = mutable.toList())
            deleteByIdAsync(removed.id)
            removed
        } catch (t: Throwable) {
            t.printStackTrace()
            setGlobalError("Could not delete the task. Please try again.")
            null
        }
    }



      //Insert a task at index,if index > size, append to end.

    fun insertTaskAt(index: Int, task: Task) {
        try {
            val tasks = uIState.tasks.toMutableList()
            val insertIndex = index.coerceIn(0, tasks.size)
            tasks.add(insertIndex, task)
            uIState = uIState.copy(tasks = tasks.toList())
            insertTaskAsync(task)
        } catch (t: Throwable) {
            t.printStackTrace()
            setGlobalError("Could not restore the task. Please try again.")
        }
    }


     //Move a task from one list index to another.

    fun moveTask(fromIndex: Int, toIndex: Int) {
        try {
            val tasks = uIState.tasks.toMutableList()
            if (fromIndex !in tasks.indices || toIndex !in 0..tasks.size) return
            if (fromIndex == toIndex) return

            // reorder in-memory
            val item = tasks.removeAt(fromIndex)
            val insertIndex = toIndex.coerceIn(0, tasks.size)
            tasks.add(insertIndex, item)

            // reassign orderIndex for every task according to new list position
            val updatedWithOrder = tasks.mapIndexed { idx, task ->
                task.copy(orderIndex = idx) // domain Task must have orderIndex field or handle mapping
            }

            // update UI immediately
            uIState = uIState.copy(tasks = updatedWithOrder)

            // persist updated order for all changed tasks
            // launch async updates for each changed task using repository
            updatedWithOrder.forEach { updatedTask ->
                updateTaskAsync(updatedTask) // updateTaskAsync must call repository.update(updatedTask)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            setGlobalError("Could not reorder tasks. Please try again.")
        }
    }

     // Mark the task at list index as COMPLETE. Returns the previous Task (so caller can undo),or null if index invalid.
    fun completeTaskAt(index: Int): Task? {
        return try {
            val tasks = uIState.tasks
            if (index < 0 || index >= tasks.size) return null
            val mutable = tasks.toMutableList()
            val prev = mutable[index]
            val updated = prev.copy(status = TaskStatus.COMPLETED)
            mutable[index] = updated
            uIState = uIState.copy(tasks = mutable.toList())
            updateTaskAsync(updated)
            prev
        } catch (t: Throwable) {
            t.printStackTrace()
            setGlobalError("Could not mark the task complete. Please try again.")
            null
        }
    }



     //Restore a previous task by replacing the task with the same id or at index
     //If index is valid, replace at index otherwise attempt to replace by id.

    fun restoreTaskAt(index: Int, previous: Task) {
        try {
            val tasks = uIState.tasks.toMutableList()
            val byId = tasks.indexOfFirst { it.id == previous.id }
            if (byId >= 0) {
                tasks[byId] = previous
            } else {
                val insertIndex = index.coerceIn(0, tasks.size)
                tasks.add(insertIndex, previous)
            }
            uIState = uIState.copy(tasks = tasks.toList())
            updateTaskAsync(previous)
        } catch (t: Throwable) {
            t.printStackTrace()
            setGlobalError("Could not restore the previous task. Please try again.")
        }
    }


// Search & Filter UI state (add inside TaskViewModel class)

    // current search query (title + description)
    private var _searchQuery by mutableStateOf("")
    val searchQuery: String get() = _searchQuery

    // whether filter panel is toggled on (checkbox). Only shown in UI when tasks.size >= 5
    private var _filterEnabled by mutableStateOf(false)
    val filterEnabled: Boolean get() = _filterEnabled

    // which priorities are selected for filtering (set contains TaskPriority names)
    private var _priorityFilters by mutableStateOf(setOf<TaskPriority>())
    val priorityFilters: Set<TaskPriority> get() = _priorityFilters

    // public updaters called from UI:
    fun updateSearchQuery(query: String) {
        _searchQuery = query
    }

    fun setFilterEnabled(enabled: Boolean) {
        _filterEnabled = enabled
    }

    // toggle a specific priority filter (called when checkbox for a priority toggles)
    fun togglePriorityFilter(priority: TaskPriority, enabled: Boolean) {
        _priorityFilters = if (enabled) {
            _priorityFilters + priority
        } else {
            _priorityFilters - priority
        }
    }





}

