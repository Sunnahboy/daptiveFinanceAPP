package com.finadapt.adaptivefinance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//A singleton database so the app does not open 50 connections and crashes the phone's memory

@Database(
    entities = [ExpenseEntity::class, AiInteractionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase(){
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        //Retrieves a singleton instance of AppDatabase, ensuring thread-safe creation
        fun getDatabase (context: Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adaptive_finance_db"
                )
                    .fallbackToDestructiveMigration() // 🟢 3: Safely wipes the old V1 database for testing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}