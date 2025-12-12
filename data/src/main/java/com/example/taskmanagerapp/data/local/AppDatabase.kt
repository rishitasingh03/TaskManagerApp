package com.example.taskmanagerapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { //only one DB instance exists
                val inst = Room.databaseBuilder( //builds a SQLite database
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_manager_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
