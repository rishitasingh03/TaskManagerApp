package com.example.taskmanagerapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    //whenever the DB changes,UI updates automatically
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    fun getAllFlow(): Flow<List<TaskEntity>>

    // One-shot read
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id ASC")
    suspend fun getAll(): List<TaskEntity>

    // Insert returns generated id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    // Bulk insert returns list of ids used for reorder updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>

    // Update returns number of rows updated
    @Update
    suspend fun update(task: TaskEntity): Int

    // Delete by entity - returns number of rows deleted
    @Delete
    suspend fun delete(task: TaskEntity): Int

    // Delete by id
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    //used when inserting new tasks
    @Query("SELECT MAX(id) FROM tasks")
    suspend fun maxIdOrNull(): Long?
}
