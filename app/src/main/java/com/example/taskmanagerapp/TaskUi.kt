package com.example.taskmanagerapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults


//wires viewmodel to ui
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp(taskViewModel: TaskViewModel) {
    val uiState = taskViewModel.uiState

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Manager") }
                )
            },
            floatingActionButton = {
                // Floating Action Button visible only when form is hidden
                if (!uiState.isFormVisible) {
                    FloatingActionButton(onClick = { taskViewModel.startAddTask() }) {
                        Text("+")
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { //callbacks passed that call viewmodel methods
                TaskManagerScreen(
                    uiState = uiState,
                    onAddTask = { taskViewModel.startAddTask() },
                    onTaskClick = { taskViewModel.startEditTask(it) },
                    onTitleChange = taskViewModel::updateTitle,
                    onDescriptionChange = taskViewModel::updateDescription,
                    onCompletionDateChange = taskViewModel::updateCompletionDate,
                    onCompletionTimeChange = taskViewModel::updateCompletionTime,
                    onPriorityChange = taskViewModel::updatePriority,
                    onStatusChange = taskViewModel::updateStatus,
                    onReminderDay1Change = taskViewModel::updateReminderDay1,
                    onReminderDay2Change = taskViewModel::updateReminderDay2,
                    onReminderDay3Change = taskViewModel::updateReminderDay3,
                    onCancel = taskViewModel::cancelEditing,
                    onSave = taskViewModel::saveTask,
                    onDelete = taskViewModel::deleteCurrentTask
                )
            }
        }
    }
}

 //Main screen combining-task list And Task form for Add or Edit

@Composable
fun TaskManagerScreen(
    uiState: TaskUiState,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCompletionDateChange: (String) -> Unit,
    onCompletionTimeChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onReminderDay1Change: (Boolean) -> Unit,
    onReminderDay2Change: (Boolean) -> Unit,
    onReminderDay3Change: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "Your Tasks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.tasks.isEmpty()) {
            Text("No tasks yet. Tap + to add a new task.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.tasks) { task ->
                    TaskListItem(task = task, onClick = { onTaskClick(task) })
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isFormVisible) {
            HorizontalDivider(thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            TaskForm(
                formState = uiState.formState,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onCompletionDateChange = onCompletionDateChange,
                onCompletionTimeChange = onCompletionTimeChange,
                onPriorityChange = onPriorityChange,
                onStatusChange = onStatusChange,
                onReminderDay1Change = onReminderDay1Change,
                onReminderDay2Change = onReminderDay2Change,
                onReminderDay3Change = onReminderDay3Change,
                onCancel = onCancel,
                onSave = onSave,
                onDelete = onDelete
            )
        }
    }
}


//task list.
@Composable
fun TaskListItem(task: Task, onClick: () -> Unit) {
    val (dateString, timeString) = splitDateAndTime(task.completionTimeMillis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = task.priority.label,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = task.description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Status: ${task.status.label}",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Completion: $dateString $timeString",
            style = MaterialTheme.typography.bodySmall
        )

        if (task.reminderDaysBefore.isNotEmpty()) {
            Text(
                text = "Reminders: ${task.reminderDaysBefore.joinToString { "$it day(s) before" }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

//add edit task form
@Composable
fun TaskForm(
    formState: TaskFormState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCompletionDateChange: (String) -> Unit,
    onCompletionTimeChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onReminderDay1Change: (Boolean) -> Unit,
    onReminderDay2Change: (Boolean) -> Unit,
    onReminderDay3Change: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val creationText = formState.creationTimeMillis?.let {
        "Created: " + formatDateTime(it)
    } ?: ""

    // Header row with creation date/time shown on top right.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        if (creationText.isNotEmpty()) {
            Text(
                text = creationText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = formState.title,
        onValueChange = { if (formState.isEditable) onTitleChange(it) },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth(),
        enabled = formState.isEditable
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = formState.description,
        onValueChange = { if (formState.isEditable) onDescriptionChange(it) },
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth(),
        enabled = formState.isEditable,
        minLines = 2
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formState.completionDate,
            onValueChange = {
                if (formState.isEditable) onCompletionDateChange(it)
            },
            label = { Text("Completion date (dd-MM-yyyy)") },
            modifier = Modifier.weight(1f),
            enabled = formState.isEditable
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = formState.completionTime,
            onValueChange = {
                if (formState.isEditable) onCompletionTimeChange(it)
            },
            label = { Text("Time (HH:mm)") },
            modifier = Modifier.weight(1f),
            enabled = formState.isEditable
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Priority dropdown
    DropdownField(
        label = "Priority",
        selectedText = formState.priority.label,
        enabled = formState.isEditable,
        options = TaskPriority.values().toList(),
        optionLabel = { it.label },
        onOptionSelected = onPriorityChange
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Status dropdown
    DropdownField(
        label = "Status",
        selectedText = formState.status.label,
        enabled = formState.isEditable,
        options = TaskStatus.values().toList(),
        optionLabel = { it.label },
        onOptionSelected = onStatusChange
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text("Reminders (up to 3 days BEFORE completion):")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = formState.reminderDay1,
            onCheckedChange = { if (formState.isEditable) onReminderDay1Change(it) },
            enabled = formState.isEditable
        )
        Text("1 day before")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = formState.reminderDay2,
            onCheckedChange = { if (formState.isEditable) onReminderDay2Change(it) },
            enabled = formState.isEditable
        )
        Text("2 days before")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = formState.reminderDay3,
            onCheckedChange = { if (formState.isEditable) onReminderDay3Change(it) },
            enabled = formState.isEditable
        )
        Text("3 days before")
    }

    Spacer(modifier = Modifier.height(8.dp))

    formState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (!formState.isEditable) {
        Text(
            text = "This task is read-only because the completion time has already passed.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        // Delete only when editing an existing task AND editable
        if (formState.taskIdBeingEdited != null && formState.isEditable) {
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        TextButton(onClick = onCancel) {
            Text("Cancel")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSave,
            enabled = formState.isEditable
        ) {
            Text("Save")
        }
    }
}

//  dropdown used for Priority and Status
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    selectedText: String,
    enabled: Boolean,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded, //menu is open or not
        onExpandedChange = {
            if (enabled) { //prevents dropdown to open when editing disabled
                expanded = !expanded
            }
        }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true, //prevents keyboard from opening
            enabled = enabled, //dropdown disabled when form not editable
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()   // the menu knows where to anchor
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false } //close menu
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}

