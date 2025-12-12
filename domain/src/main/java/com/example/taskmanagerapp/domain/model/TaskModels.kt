package com.example.taskmanagerapp.domain.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


//Priority of a task.
enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}


// Status of a task.

enum class TaskStatus(val label: String) {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed")
}


//Entity representing a Task in the app.
data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val completionTimeMillis: Long,
    val creationTimeMillis: Long,
    val priority: TaskPriority,
    val status: TaskStatus,
    val reminderDaysBefore: List<Int>, // number of days before completion time (1,2,3).
    val orderIndex: Int = 0 // list ordering index (0 = top)
)


 //UI state for the task form (Add or Edit).
data class TaskFormState(
     val taskIdBeingEdited: Long? = null, // null = new task
     val title: String = "",
     val description: String = "",
     val completionDate: String = "",
     val completionTime: String = "",
     val priority: TaskPriority = TaskPriority.MEDIUM,
     val status: TaskStatus = TaskStatus.NOT_STARTED,
     val reminderDay1: Boolean = false,
     val reminderDay2: Boolean = false,
     val reminderDay3: Boolean = false,
     val creationTimeMillis: Long? = null,
     val isEditable: Boolean = true,      //false if deadline already passed
     val errorMessage: String? = null //validation error
)


  //High-level UI state of the screen.

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isFormVisible: Boolean = false,
    val formState: TaskFormState = TaskFormState(),
    val errorMessage: String? = null //error message for UI to show
)

// Date/time helper functions

private val dateTimeSdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
private val dateSdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
private val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())


 //Convert date ("dd-MM-yyyy") and time ("HH:mm") into millis.
fun parseToMillis(date: String, time: String): Long? {
    return try {
        dateTimeSdf.parse("$date $time")?.time
    } catch (e: ParseException) {
        null   //Returns null if parsing fails.
    }
}

//Split millis into date and time strings.
fun splitDateAndTime(millis: Long): Pair<String, String> {
    val date = Date(millis)
    return dateSdf.format(date) to timeSdf.format(date)
}

//Format millis for display as "dd-mm-yyyy HH:MM".
fun formatDateTime(millis: Long): String {
    val date = Date(millis)
    return dateTimeSdf.format(date)
}
