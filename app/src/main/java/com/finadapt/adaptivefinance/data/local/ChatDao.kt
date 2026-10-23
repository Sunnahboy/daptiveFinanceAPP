package com.finadapt.adaptivefinance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllThreads(): Flow<List<ChatThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThreadEntity)

    @Query("UPDATE chat_threads SET title = :newTitle, updatedAt = :timestamp WHERE threadId = :threadId")
    suspend fun updateThreadTitle(threadId: String, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chat_threads SET isPinned = :isPinned, updatedAt = :timestamp WHERE threadId = :threadId")
    suspend fun updateThreadPinned(threadId: String, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chat_threads SET updatedAt = :timestamp WHERE threadId = :threadId")
    suspend fun updateThreadTimestamp(threadId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_threads WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    suspend fun getMessagesForThread(threadId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: String)
}
