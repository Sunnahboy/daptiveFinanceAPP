package com.finadapt.adaptivefinance.data.repository

import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.local.AiInteractionEntity
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.data.remote.ContextRequest
import com.finadapt.adaptivefinance.data.remote.AiResponse
import com.finadapt.adaptivefinance.data.remote.FeedbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.SharedPreferences
import androidx.core.content.edit

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) {

    private val apiService = ApiClient.fastApiService

    suspend fun logExpenseAndGetStrategy(
        userId: String,
        amount: Float,
        category: String = "General"
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. SAVE TO DEVICE BRAIN (Room SQLite)
                //We must save the expense to the database BEFORE we do the math
                val newExpense = ExpenseEntity(
                    amount = amount,
                    category = category,
                    timestamp = System.currentTimeMillis() // 🟢 FIX: Stamps it with today's date!
                )
                expenseDao.insertExpense(newExpense)
                // 2. EDGE FEATURE ENGINEERING (With Projected Run Rate!)

                // Fetch the 30-Day Bounded Spend (from your DAO)
                val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
                val timeLimit = System.currentTimeMillis() - thirtyDaysInMillis
                val totalSpend = expenseDao.getTotalSpendTimeBounded(timeLimit) ?: 0f
                val txCount = expenseDao.getTransactionCount().toFloat()
                val avgTxValue = if (txCount > 0f) totalSpend / txCount else 0f

                // Fetch Baseline & Install Date
                val monthlyBudget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                val installTimestamp = prefs.getLong("INSTALL_TIMESTAMP", System.currentTimeMillis())

                // Calculate "Days Active"
                val millisActive = System.currentTimeMillis() - installTimestamp
                val daysActive = maxOf(1f, millisActive / (1000f * 60f * 60f * 24f))

                // 🟢 THE FIX: Calculate the Projected 30-Day Spend
                val projectedSpend = if (daysActive < 30f) {
                    // New User: Extrapolate their current pace to a full month
                    (totalSpend / daysActive) * 30f
                } else {
                    // Veteran User: The 30-day window is already fully accurate
                    totalSpend
                }

                // Final Volatility Math using the PROJECTED spend
                val spendingVolatility = if (monthlyBudget > 0f) {
                    (projectedSpend / monthlyBudget).coerceIn(0.0f, 2.0f)
                } else {
                    0.5f
                }

                // 3. SEND THE ANONYMIZED VECTOR TO AWS
                val request = ContextRequest(
                    userId = userId,
                    testGroup = "adaptive",     //tell the DB they are in the AI group
                    amount = amount,            //Pass the amount down
                    category = category,        //Pass the category down
                    features = mapOf(
                        "total_spend" to totalSpend,
                        "spending_volatility" to spendingVolatility,
                        "return_rate" to 0.05f, // Mocked for now
                        "transaction_count" to txCount,
                        "avg_transaction_value" to avgTxValue
                    )

                )

                val response = apiService.getAiGamification(ApiClient.API_TOKEN, request)
                //If AWS doesn't send an ID, we generate a safe local fallback UUID
                val currentPredictionId = response.predictionId ?: "fallback_${java.util.UUID.randomUUID()}"
                val aiMemory = AiInteractionEntity(
                    predictionId = currentPredictionId,
                    strategy = response.recommendedStrategy ?: "Standard",
                    action = response.action ?: "log_only",
                    notification = response.gamificationMessage ?: "Expense logged.",
                    visualTheme = response.visualTheme ?: "Neutral"
                )
                expenseDao.insertAiInteraction(aiMemory)
                Result.success(response)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitUserFeedback(predictionId: String, reward: Int) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Send to AWS for the bandit feedback(0 or 1)
                val feedback = FeedbackRequest(predictionId, reward)
                apiService.sendFeedback(ApiClient.API_TOKEN, feedback)

                // 2. If successful, update the local database so we never send it again!
                expenseDao.markFeedbackAsSent(predictionId)
                //3 XP reward the user for playing the game
                if(reward > 0f){
                    // reward > 0 users clicked yes or accepted challenge
                    val currentXp = prefs.getInt("USER_XP", 0)
                    prefs.edit { putInt("USER_XP", currentXp + 50) }
                }

            } catch (e: Exception) {
                // If the network fails, we do nothing!
                // isFeedbackSent remains false in Room, and WorkManager will catch it later.
                println("⚠️ Feedback Sync Paused. Reason: ${e.localizedMessage}")
                println("Network failed. Feedback saved locally for eventual sync.")
            }
        }
    }
}