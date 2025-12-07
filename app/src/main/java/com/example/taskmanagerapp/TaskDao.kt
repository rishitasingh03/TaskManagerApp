package com.example.taskmanagerapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllFlow(): Flow<List<TaskEntity>> //whenever the DB changes,UI updates automatically

    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity) // insert task

    //used for reorder updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity) // update task

    @Delete
    suspend fun delete(task: TaskEntity) //delete task

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    //used when inserting new tasks
    @Query("SELECT MAX(id) FROM tasks")
    suspend fun maxIdOrNull(): Long?
}
