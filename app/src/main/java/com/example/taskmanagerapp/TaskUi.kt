package com.example.taskmanagerapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarDuration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.filled.Add
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.model.TaskFormState
import com.example.taskmanagerapp.domain.model.TaskPriority
import com.example.taskmanagerapp.domain.model.TaskStatus
import com.example.taskmanagerapp.domain.model.TaskUiState
import com.example.taskmanagerapp.domain.model.formatDateTime
import com.example.taskmanagerapp.domain.model.splitDateAndTime
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController


// Root composable of the task feature.
// Responsible only for wiring ViewModel state + events to UI composables.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp(taskViewModel: TaskViewModel) {
    val uiState = taskViewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    MaterialTheme {
        // Scaffold provides top bar, FAB, snackbar host and main content layout.
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Task Manager") }) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (!uiState.isFormVisible) {
                    // Floating action button is shown only when task form is not visible.
                    // It triggers the add new task flow through ViewModel.
                    FloatingActionButton(
                        onClick = { taskViewModel.startAddTask() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main screen composable
                // ViewModel-derived state is passed down to keep UI stateless and declarative.
                TaskManagerScreen(
                    uiState = uiState,
                    displayedTasks = taskViewModel.displayedTasks,
                    showFilterToggle = taskViewModel.shouldShowFilter,
                    emptyStateMessage = taskViewModel.emptyStateMessage,
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
                    onDelete = taskViewModel::deleteCurrentTask,
                    onMove = { from, to -> taskViewModel.moveTask(from, to) },
                    onDeleteAt = { idx -> taskViewModel.deleteTaskAt(idx) },
                    onInsertAt = { idx, task -> taskViewModel.insertTaskAt(idx, task) },
                    onCompleteAt = { idx -> taskViewModel.completeTaskAt(idx) },
                    onRestoreAt = { idx, prev -> taskViewModel.restoreTaskAt(idx, prev) },
                    snackbarHostState = snackbarHostState,
                    onClearError = { taskViewModel.clearError() },
                    searchQuery = taskViewModel.searchQuery,
                    onSearchQueryChange = taskViewModel::updateSearchQuery,
                    filterEnabled = taskViewModel.filterEnabled,
                    onFilterEnabledChange = taskViewModel::setFilterEnabled,
                    priorityFilters = taskViewModel.priorityFilters,
                    onTogglePriorityFilter = taskViewModel::togglePriorityFilter
                )
            }
        }
    }
}
// High-level screen composable.
// Composes smaller feature-specific UI blocks (search, filter, list, form)
@Composable
fun TaskManagerScreen(
    uiState: TaskUiState,
    displayedTasks: List<Task>,
    showFilterToggle: Boolean,
    emptyStateMessage: String?,
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
    onDelete: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDeleteAt: (Int) -> Task?,
    onInsertAt: (Int, Task) -> Unit,
    onCompleteAt: (Int) -> Task?,
    onRestoreAt: (Int, Task) -> Unit,
    snackbarHostState: SnackbarHostState,
    onClearError: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterEnabled: Boolean,
    onFilterEnabledChange: (Boolean) -> Unit,
    priorityFilters: Set<TaskPriority>,
    onTogglePriorityFilter: (TaskPriority, Boolean) -> Unit
) {

    // Snackbar error handling (UI side-effect)
    LaunchedEffect(uiState.formState.errorMessage) {
        uiState.formState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }
// Vertical layout for main screen content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        TaskHeader()

        Spacer(Modifier.height(8.dp))

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange
        )

        Spacer(Modifier.height(8.dp))

        FilterSection(
            showFilterToggle = showFilterToggle,
            filterEnabled = filterEnabled,
            priorityFilters = priorityFilters,
            onFilterEnabledChange = onFilterEnabledChange,
            onTogglePriorityFilter = onTogglePriorityFilter
        )

        Spacer(Modifier.height(8.dp))

        TaskListSection(
            displayedTasks = displayedTasks,
            emptyStateMessage = emptyStateMessage,
            onTaskClick = onTaskClick,
            onMove = onMove,
            onDeleteAt = onDeleteAt,
            onInsertAt = onInsertAt,
            onCompleteAt = onCompleteAt,
            onRestoreAt = onRestoreAt,
            snackbarHostState = snackbarHostState
        )
    }

    TaskFormOverlay(
        uiState = uiState,
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

// Simple header displaying screen title.
@Composable
fun TaskHeader() {
    Text(
        text = "Your Tasks",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

// Search input field
// Emits user query changes to ViewModel, does not perform filtering itself
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {// for controlling keyboard
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search tasks") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        //// Keyboard action hides the keyboard on search for better UX.
        keyboardActions = KeyboardActions { keyboardController?.hide() }
    )
}

// Filter controls for task list
// Visibility and filtering rules are decided by ViewModel and passed as parameters
@Composable
fun FilterSection(
    showFilterToggle: Boolean,
    filterEnabled: Boolean,
    priorityFilters: Set<TaskPriority>,
    onFilterEnabledChange: (Boolean) -> Unit,
    onTogglePriorityFilter: (TaskPriority, Boolean) -> Unit
) {// Do not render filter UI if ViewModel says filtering is not applicable
    if (!showFilterToggle) return

    Column {
        Button(onClick = { onFilterEnabledChange(!filterEnabled) }) {
            Text("Filter by Priority")
        }

        if (filterEnabled) {
            Row {
                TaskPriority.values().forEach { priority ->
                    // Priority checkboxes allow multi-select filtering
                    // Actual filtering logic is handled in ViewModel
                    Checkbox(
                        checked = priorityFilters.contains(priority),
                        onCheckedChange = {
                            onTogglePriorityFilter(priority, it)
                        }
                    )
                    Text(priority.label)
                }
            }
        }
    }
}

// Displays either the task list or an empty state message
// The decision of what message to show is made in ViewModel
@Composable
fun TaskListSection(
    displayedTasks: List<Task>,
    emptyStateMessage: String?,
    onTaskClick: (Task) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDeleteAt: (Int) -> Task?,
    onInsertAt: (Int, Task) -> Unit,
    onCompleteAt: (Int) -> Task?,
    onRestoreAt: (Int, Task) -> Unit,
    snackbarHostState: SnackbarHostState
) {// Empty state UI when there are no tasks to display
    if (emptyStateMessage != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyStateMessage)
        }
        // Task list with swipe and drag interactions
    } else {
        ReorderableTaskList(
            modifier = Modifier.fillMaxSize(),
            tasks = displayedTasks,
            onTaskClick = onTaskClick,
            onMove = onMove,
            onDeleteAt = onDeleteAt,
            onInsertAt = onInsertAt,
            onCompleteAt = onCompleteAt,
            onRestoreAt = onRestoreAt,
            snackbarHostState = snackbarHostState
        )
    }
}

// Full-screen overlay for creating or editing a task
// Visibility and editability are controlled by ViewModel state
@Composable
fun TaskFormOverlay(
    uiState: TaskUiState,
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
) { // Do not render form when not active
    if (!uiState.isFormVisible) return

    Surface(
        modifier = Modifier.fillMaxSize(),
        tonalElevation = 8.dp
    ) {
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


 // When long-press drag starts, track vertical drag distance (dy).
 // Every time dy crosses one item height, move the item up/down by 1 index.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReorderableTaskList(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDeleteAt: (index: Int) -> Task?,
    onInsertAt: (index: Int, task: Task) -> Unit,
    onCompleteAt: (index: Int) -> Task?,
    onRestoreAt: (index: Int, previous: Task) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    if (tasks.size <= 1) { //if list has 0 or 1 item no drag needed
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            itemsIndexed(tasks) { _, t ->
                TaskListItem(task = t, onClick = { onTaskClick(t) })
                HorizontalDivider()
            }
        }
        return
    }

    var draggingId by remember { mutableStateOf<Long?>(null) } // id of current task that is dragged
    var measuredItemPx by remember { mutableStateOf<Float?>(null) } // row height
    val fallbackItemPx = with(LocalDensity.current) { 120.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(tasks, key = { _, t -> t.id }) { index, task ->

                var itemWidthPx by remember { mutableStateOf(0f) }

                val offsetX = remember { Animatable(0f) }
                var isDismissed by remember { mutableStateOf(false) } //hide item during swipe

                val swipeThresholdPx = itemWidthPx * 0.35f //swipe must pass 35% width to trigger action

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            val height = coords.size.height.toFloat()
                            if (height > 0f) measuredItemPx = height
                            itemWidthPx = coords.size.width.toFloat()
                        }
                ) {
                    // SWIPE handling - horizontal drags
                    val swipeModifier = Modifier.pointerInput(task.id, draggingId) {
                        // if a drag reorder is active, swipe detection not installed for this item
                        if (draggingId != null) return@pointerInput

                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var dragConsumed = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.changedToUp()) break

                                val delta = change.positionChange()
                                if (delta != Offset.Zero) {
                                    if (!dragConsumed && kotlin.math.abs(delta.x) > kotlin.math.abs(delta.y)) {
                                        dragConsumed = true
                                    }

                                    if (dragConsumed) {
                                        val newValue = (offsetX.value + delta.x)
                                            .coerceIn(-itemWidthPx * 2f, itemWidthPx * 2f) //swipe distance
                                        coroutineScope.launch { offsetX.snapTo(newValue) } //instantly move ui item
                                        change.consume() //prevent scroll conflict
                                    }
                                }
                            }

                            // decide on release: left swipe -> delete, right swipe -> complete, else reset
                            val finalX = offsetX.value

                            if (kotlin.math.abs(finalX) > swipeThresholdPx) {
                                isDismissed = true //hide item temporarily

                                if (finalX < 0f) { //swipe left
                                    // LEFT swipe -> Delete
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            -itemWidthPx * 1.2f,
                                            animationSpec = tween(200) //animate swipe out
                                        )
                                        val removed = onDeleteAt(index) // remove task immediately
                                        //snackbar with undo option
                                        if (removed != null) {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Task deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onInsertAt(index.coerceIn(0, tasks.size), removed) //undo deletion
                                            }
                                        }
                                        isDismissed = false
                                        offsetX.snapTo(0f) //reset position after action
                                    }
                                } else {
                                    // RIGHT swipe -> Complete
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            itemWidthPx * 1.2f,
                                            animationSpec = tween(200) //animate right
                                        )
                                        val previous = onCompleteAt(index) //mark as complete
                                        if (previous != null) {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Task marked complete",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onRestoreAt(
                                                    index.coerceIn(0, tasks.size),
                                                    previous
                                                )
                                            }
                                        }
                                        isDismissed = false
                                        offsetX.snapTo(0f)
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, animationSpec = tween(200))// snap back to original position
                                }
                            }
                        } // awaitEachGesture
                    }

                    // REORDER handler: long-press + vertical drag
                    val reorderModifier = Modifier.pointerInput(task.id) {
                        var accumulatedDy = 0f //sum of vertical deltas while user drags

                        detectDragGesturesAfterLongPress(
                            onDragStart = { //after long press drag starts
                                draggingId = task.id
                                accumulatedDy = 0f
                            },
                            onDragEnd = { //handle drag end
                                draggingId = null
                                accumulatedDy = 0f
                            },
                            onDragCancel = { //handle drag cancellation
                                draggingId = null
                                accumulatedDy = 0f
                            },
                            //handle ongoing drag movements
                            onDrag = { change: PointerInputChange, dragAmount ->
                                change.consume()

                                val heightPx = measuredItemPx ?: fallbackItemPx
                                if (heightPx <= 0f) return@detectDragGesturesAfterLongPress

                                // accumulate vertical drag distance
                                accumulatedDy += dragAmount.y

                                // how many whole item-heights have crossed
                                val indexChange = (accumulatedDy / heightPx).toInt()
                                if (indexChange != 0) {
                                    val currentIndex =
                                        tasks.indexOfFirst { it.id == task.id }
                                    if (currentIndex >= 0) {
                                        val newIndex = (currentIndex + indexChange)
                                            .coerceIn(0, tasks.size - 1)
                                        if (newIndex != currentIndex) {
                                            onMove(currentIndex, newIndex)
                                        }
                                    }
                                    // subtract the amount used to move items,
                                    // so small extra movement doesn't cause double moves
                                    accumulatedDy -= indexChange * heightPx
                                }
                            }
                        )
                    }

                    // start with base
                    var finalModifier: Modifier = Modifier.fillMaxWidth()

                    // only animate placement when this item is NOT being dragged
                    if (draggingId != task.id) {
                        finalModifier = finalModifier.animateItemPlacement(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }

                    // Horizontal swipe offset + reorder + visuals
                    finalModifier = finalModifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .then(reorderModifier)
                        .then(swipeModifier)
                        .zIndex(if (draggingId == task.id) 1f else 0f)
                        .background(
                            if (draggingId == task.id)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)

                    if (!isDismissed) {
                        Box(modifier = finalModifier) {
                            TaskListItem(task = task, onClick = { onTaskClick(task) })
                        }
                    }
                } // item Box

                HorizontalDivider()
            } // itemsIndexed
        } // LazyColumn
    } // Box
}

// Visual representation of a single task in the list. Displays title, priority, status, completion time and reminder info.
@Composable
fun TaskListItem(task: Task, onClick: () -> Unit) {
    val (dateString, timeString) = splitDateAndTime(task.completionTimeMillis)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 4.dp)
            .height(100.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp)
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
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = task.priority.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

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
                    text = "Reminders: ${
                        task.reminderDaysBefore.joinToString { "$it day(s) before" }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Task creation/editing form
// UI reflects form state from ViewModel and forwards user actions via callbacks
@Composable
fun TaskForm(
    modifier: Modifier = Modifier,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        val creationText =
            formState.creationTimeMillis?.let { "Created: " + formatDateTime(it) } ?: ""
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (creationText.isNotEmpty()) {
                Text(text = creationText, style = MaterialTheme.typography.labelSmall)
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
                onValueChange = { if (formState.isEditable) onCompletionDateChange(it) },
                label = { Text("Completion date (dd-MM-yyyy)") },
                modifier = Modifier.weight(1f),
                enabled = formState.isEditable
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = formState.completionTime,
                onValueChange = { if (formState.isEditable) onCompletionTimeChange(it) },
                label = { Text("Time (HH:mm)") },
                modifier = Modifier.weight(1f),
                enabled = formState.isEditable
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        DropdownField(
            label = "Priority",
            selectedText = formState.priority.label,
            enabled = formState.isEditable,
            options = TaskPriority.values().toList(),
            optionLabel = { it.label },
            onOptionSelected = { selected -> onPriorityChange(selected) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DropdownField(
            label = "Status",
            selectedText = formState.status.label,
            enabled = formState.isEditable,
            options = TaskStatus.values().toList(),
            optionLabel = { it.label },
            onOptionSelected = { selected -> onStatusChange(selected) }
        )

        Spacer(modifier = Modifier.height(12.dp))
// Reminder selection (up to 3 days before completion)
        Text("Reminders (up to 3 days BEFORE completion):")
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = formState.reminderDay1,
                onCheckedChange = { if (formState.isEditable) onReminderDay1Change(it) },
                enabled = formState.isEditable
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("1 day before")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = formState.reminderDay2,
                onCheckedChange = { if (formState.isEditable) onReminderDay2Change(it) },
                enabled = formState.isEditable
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("2 days before")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = formState.reminderDay3,
                onCheckedChange = { if (formState.isEditable) onReminderDay3Change(it) },
                enabled = formState.isEditable
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("3 days before")
        }

        Spacer(modifier = Modifier.height(12.dp))

        formState.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (formState.taskIdBeingEdited != null && formState.isEditable) {
                TextButton(onClick = onDelete) { Text("Delete") }
                Spacer(modifier = Modifier.width(8.dp))
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSave, enabled = formState.isEditable) { Text("Save") }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
//drop down option for priority and status selection
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
    val keyForSave = selectedText
    val expandedState = rememberSaveable(keyForSave) { mutableStateOf(false) }
    val expanded by remember { derivedStateOf { expandedState.value } }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) {
                expandedState.value = !expandedState.value
            }
        }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    expandedState.value = true
                }
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expandedState.value = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expandedState.value = false
                        onOptionSelected(option)
                    }
                )
            }
        }
    }


}
