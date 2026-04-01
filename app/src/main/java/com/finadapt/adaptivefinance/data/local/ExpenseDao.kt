package com.finadapt.adaptivefinance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
data class CategorySum(
    val category: String,
    val total: Float
)
@Dao
interface ExpenseDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalSpend(): Float?

    @Query("SELECT COUNT(id) FROM expenses")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getALLExpensesFlow(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAiInteraction(interaction: AiInteractionEntity)

    @Query("UPDATE ai_interactions SET isFeedbackSent = 1 WHERE predictionId = :predictionId")
    suspend fun markFeedbackAsSent(predictionId: String)

    @Query("SELECT * FROM ai_interactions WHERE isFeedbackSent = 0")
    suspend fun getPendingFeedback(): List<AiInteractionEntity>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 3")
    suspend fun getRecentLedger(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startOfDay")
    suspend fun getTodaySpend(startOfDay: Long): Float?

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    // 🟢 Restored: Fetch the Bandit AI's recent gamification actions
    @Query("SELECT * FROM ai_interactions")
    suspend fun getRecentAiInteractions(): List<AiInteractionEntity>

    // ====================================================================
    // 🤖 CHATBOT BOUNDED QUERIES (Using Exact Start and End Times)
    // ====================================================================

    // 1. Total Spend Bounded
    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpendTimeBounded(startTime: Long, endTime: Long): Float?

    // 2. Total Spend by Category Bounded
    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpendByCategoryAndTime(category: String, startTime: Long, endTime: Long): Float?

    // 3. Get Expenses List Bounded
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getExpensesSince(startTime: Long, endTime: Long): List<ExpenseEntity>

    // NOTE: Kept your original single-parameter one just in case your Bar Charts are still using it!
    @Query("SELECT * FROM expenses WHERE timestamp >= :timeLimit ORDER BY timestamp ASC")
    suspend fun getExpensesSince(timeLimit: Long): List<ExpenseEntity>

    // 4. Get Expenses by Category List Bounded
    @Query("SELECT * FROM expenses WHERE category = :category AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getExpensesByCategoryAndTimeList(category: String, startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY category")
    suspend fun getCategoryBreakdown(startTime: Long, endTime: Long): List<CategorySum>
    // 🟢 NEW: Keyword Search Engine
    @Query("SELECT * FROM expenses WHERE (merchantName LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun searchExpenses(searchQuery: String, startTime: Long, endTime: Long): List<ExpenseEntity>
    // 🟢 NEW: Finds the single most expensive item purchased in a given timeframe
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY amount DESC LIMIT 1")
    suspend fun getHighestExpense(startTime: Long, endTime: Long): ExpenseEntity?


}