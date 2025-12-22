package com.example.taskmanagerapp

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.taskmanagerapp.domain.model.Task
import com.example.taskmanagerapp.domain.model.TaskPriority
import com.example.taskmanagerapp.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test


//Unit tests for TaskViewModel
//These tests validate business logic 

class TaskViewModelTest {
    // Executes LiveData and state updates instantly in unit tests.
    // This avoids threading issues during assertions.
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TaskViewModel
    // Test dispatcher to control coroutine execution manually
    private val testDispatcher = StandardTestDispatcher()


    // TEST SETUP


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        // Replace Main dispatcher with a test dispatcher
        // This ensures coroutines run in a controlled test environment
        Dispatchers.setMain(testDispatcher)
        //fake repository so tests do not touch the real database
        val fakeRepo = FakeTaskRepository()
        // Application context required because TaskViewModel extends AndroidViewModel
        val application = Application()
        // ViewModel created with fake repository and DB sync disabled
        viewModel = TaskViewModel(
            application = application,
            repository = fakeRepo,
            enableDbSync = false
        )
    }

    @After
    fun tearDown() {
        // Restore the original Main dispatcher after each test
        Dispatchers.resetMain()
    }

    // HELPER FUNCTIONS

//// Creates a sample Task object for testing.
//    // Helps avoid repeating task construction code in every test
    private fun createTask(
        id: Long = 1L,
        title: String = "Test Task",
        description: String = "Test Description",
        priority: TaskPriority = TaskPriority.MEDIUM,
        status: TaskStatus = TaskStatus.NOT_STARTED,
        completionTimeMillis: Long = System.currentTimeMillis() + 60_000,
        creationTimeMillis: Long = System.currentTimeMillis(),
        reminderDaysBefore: List<Int> = emptyList(),
        orderIndex: Int = 0
    ): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            completionTimeMillis = completionTimeMillis,
            creationTimeMillis = creationTimeMillis,
            priority = priority,
            status = status,
            reminderDaysBefore = reminderDaysBefore,
            orderIndex = orderIndex
        )
    }


    // SEARCH TESTS


    @Test
    fun search_filters_tasks_by_title() {
        // Given a list of tasks with different titles
        val tasks = listOf(
            createTask(id = 1, title = "Buy Milk"),
            createTask(id = 2, title = "Workout")
        )
// Set tasks directly into ViewModel for testing
        viewModel.setTasksForTest(tasks)
// When searching by a keyword present in the title
        val result = viewModel.filterTasksForTest(
            query = "milk",
            filterEnabled = false,
            high = false,
            medium = false,
            low = false
        )
// only matching tasks should be returned
        assertEquals(1, result.size)
        assertEquals("Buy Milk", result.first().title)
    }

    @Test
    fun search_filters_tasks_by_description() {
        //task with a searchable description
        val tasks = listOf(
            createTask(id = 1, description = "Office meeting")
        )

        viewModel.setTasksForTest(tasks)
//searching using description text
        val result = viewModel.filterTasksForTest(
            query = "office",
            filterEnabled = false,
            high = false,
            medium = false,
            low = false
        )
//the task should appear in results
        assertEquals(1, result.size)
    }


    // FILTER TESTS


    @Test
    fun filter_returns_only_high_priority_tasks() {
        //tasks with different priorities
        val tasks = listOf(
            createTask(id = 1, priority = TaskPriority.HIGH),
            createTask(id = 2, priority = TaskPriority.LOW)
        )

        viewModel.setTasksForTest(tasks)

        //when filter is enabled and only HIGH priority is selected
        val result = viewModel.filterTasksForTest(
            query = "",
            filterEnabled = true,
            high = true,
            medium = false,
            low = false
        )
//only HIGH priority tasks should be returned
        assertEquals(1, result.size)
        assertEquals(TaskPriority.HIGH, result.first().priority)
    }

    @Test
    fun filter_with_no_priority_selected_returns_all_tasks() {
        //multiple tasks with different priorities
        val tasks = listOf(
            createTask(id = 1, priority = TaskPriority.HIGH),
            createTask(id = 2, priority = TaskPriority.MEDIUM)
        )

        viewModel.setTasksForTest(tasks)
// When filter is enabled but no priority is selected
        val result = viewModel.filterTasksForTest(
            query = "",
            filterEnabled = true,
            high = false,
            medium = false,
            low = false
        )
// all tasks should be returned
        assertEquals(2, result.size)
    }


    // DELETE TEST


    @Test
    fun deleteTask_removes_task_from_list() {
        //a single task in ViewModel
        val task = createTask()

        viewModel.setTasksForTest(listOf(task))
        // When the task is deleted by index
        viewModel.deleteTaskAt(0)
        // Then the task list should be empty
        assertTrue(viewModel.uiState.tasks.isEmpty())
    }


    // VALIDATION TEST


    @Test
    fun saveTask_with_empty_title_sets_error() {
        //user starts adding a task
        viewModel.startAddTask()
        // When title is empty and save is attempted
        viewModel.updateTitle("")

        viewModel.saveTask()
        // Then validation error should be set
        assertNotNull(viewModel.uiState.formState.errorMessage)
    }


    // REORDER TEST


    @Test
    fun moveTask_reorders_tasks_correctly() {
        //given two tasks in a specific order
        val task1 = createTask(id = 1, title = "Task 1")
        val task2 = createTask(id = 2, title = "Task 2")

        viewModel.setTasksForTest(listOf(task1, task2))
        // When first task is moved below the second
        viewModel.moveTask(0, 1)
        // Then the order should be updated correctly
        assertEquals("Task 2", viewModel.uiState.tasks[0].title)
        assertEquals("Task 1", viewModel.uiState.tasks[1].title)
    }

    //BIOMETRIC LOGIC TESTS

    @Test
    fun app_content_is_hidden_when_not_authenticated() {
        // Given user is not authenticated
        val result = viewModel.canShowTasks(false)

        // Then task list should not be shown
        assertFalse(result)
    }

    @Test
    fun app_content_is_visible_when_authenticated() {
        // Given user is authenticated
        val result = viewModel.canShowTasks(true)

        // Then task list should be visible
        assertTrue(result)
    }

    // REMINDER SCHEDULER LOGIC TESTS
    // These tests validate reminder calculation logic.
// They do NOT test AlarmManager or system notifications.

    @Test
    fun high_priority_task_generates_daily_reminder_marker() {
        // Given a HIGH priority task
        val task = createTask(
            priority = TaskPriority.HIGH,
            reminderDaysBefore = emptyList(),
            completionTimeMillis = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000
        )

        // When computing reminder times
        val result = com.example.taskmanagerapp.notification.computeReminderTimes(task)

        // Then daily reminder marker should be present
        assertTrue(result.contains(-1L))
    }

    @Test
    fun reminder_days_generate_correct_number_of_reminders() {
        // Given a task with 1 and 2 day reminder selections
        val task = createTask(
            priority = TaskPriority.MEDIUM,
            reminderDaysBefore = listOf(1, 2),
            completionTimeMillis = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000
        )

        val result = com.example.taskmanagerapp.notification.computeReminderTimes(task)

        // Two reminder times should be generated
        assertEquals(2, result.size)
    }

    @Test
    fun past_reminder_times_are_not_generated() {
        // Given a task completing tomorrow
        val task = createTask(
            priority = TaskPriority.LOW,
            reminderDaysBefore = listOf(2),
            completionTimeMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        )

        val result = com.example.taskmanagerapp.notification.computeReminderTimes(task)

        // Reminder scheduled in the past should be skipped
        assertTrue(result.isEmpty())
    }
}

