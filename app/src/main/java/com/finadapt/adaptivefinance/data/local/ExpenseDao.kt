package com.finadapt.adaptivefinance.data.local

import androidx.room.Dao //Data access Object
import  androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow //manage async data streams

@Dao
interface  ExpenseDao{
    // 1. save a new expense
    @Insert
    suspend fun  insertExpense(expense: ExpenseEntity)

    // 2. Get the total amount spent ( For bandit context Vector )
    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun  getTotalSpend(): Float?
    //The 30-Day Rolling Window Query
    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :timeLimit")
    suspend fun getTotalSpendTimeBounded(timeLimit: Long): Float?

    //3. Get total number of transactions (fo bandit context vector)
    @Query("SELECT COUNT(id) FROM expenses")
    suspend fun getTransactionCount(): Int
    //4 Get a live stream of all expenses (for UI history list later)
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getALLExpensesFlow(): Flow<List<ExpenseEntity>>


    // 5. Save the AI's prediction the exact second it arrives from AWS
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAiInteraction(interaction: AiInteractionEntity)

    // 6 Mark it as "Sent" when the user clicks the button
    @Query("UPDATE ai_interactions SET isFeedbackSent = 1 WHERE predictionId = :predictionId")
    suspend fun markFeedbackAsSent(predictionId: String)

    // 7 Find all pending feedback (For Phase 2: WorkManager Sync)
    @Query("SELECT * FROM ai_interactions WHERE isFeedbackSent = 0")
    suspend fun getPendingFeedback(): List<AiInteractionEntity>
}
