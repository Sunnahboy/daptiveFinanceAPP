package com.finadapt.adaptivefinance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // 🟢 FIX 1: Added REPLACE strategy so this acts as BOTH "Create" and "Edit"
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    // 🟢 FIX 2: Added the Delete command!
    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    // 2. Get the total amount spent ( For bandit context Vector )
    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalSpend(): Float?

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

    //Get raw expenses from the last 7 days for the Bar Chart
    @Query("SELECT * FROM expenses WHERE timestamp >= :timeLimit ORDER BY timestamp ASC")
    suspend fun getExpensesSince(timeLimit: Long): List<ExpenseEntity>

    //Get exactly the 3 newest expenses for the Ledger Preview
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 3")
    suspend fun getRecentLedger(): List<ExpenseEntity>

    //Get absolutely every expense for the History Tab
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    //Sum all expenses from the start of the day
    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startOfDay")
    suspend fun getTodaySpend(startOfDay: Long): Float?

    //The Factory Reset (Deletes all expenses)
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
    // 🟢 NEW: Fetch the Bandit AI's recent gamification actions
    @Query("SELECT * FROM ai_interactions")
    suspend fun getRecentAiInteractions(): List<AiInteractionEntity>
}