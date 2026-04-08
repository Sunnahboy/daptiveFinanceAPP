package com.finadapt.adaptivefinance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/*  A singleton database so the app does not open
    50 connections and crashes the phone's memory
 */

@Database(
    entities = [ExpenseEntity::class, AiInteractionEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class) //Gson converter for Room
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
                    .fallbackToDestructiveMigration() //Safely wipes the old V1 database for testing
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}