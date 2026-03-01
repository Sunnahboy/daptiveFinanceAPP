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

class FinanceRepository(private val expenseDao: ExpenseDao) {

    private val apiService = ApiClient.fastApiService

    suspend fun logExpenseAndGetStrategy(
        userId: String,
        amount: Float,
        category: String = "General"
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. SAVE TO DEVICE BRAIN (Room SQLite)
                val newExpense = ExpenseEntity(amount = amount, category = category)
                expenseDao.insertExpense(newExpense)

                // 2. EDGE FEATURE ENGINEERING (Calculate on device for privacy/speed)
                val totalSpend = expenseDao.getTotalSpend() ?: 0f
                val txCount = expenseDao.getTransactionCount().toFloat()

                val avgTxValue = if (txCount > 0f) totalSpend / txCount else 0f

                //Edge Volatility Math: Spike the volatility if they spend 2x their normal average
                val spendingVolatility = if (avgTxValue > 0f) {
                    val ratio = amount / avgTxValue
                    (ratio * 0.5f).coerceIn(0.1f, 1.5f)
                } else {
                    0.5f
                }

                // 3. SEND THE ANONYMIZED VECTOR TO AWS
                val request = ContextRequest(
                    userId = userId,
                    features = mapOf(
                        "total_spend" to totalSpend,
                        "spending_volatility" to spendingVolatility,
                        "return_rate" to 0.05f, // Mocked for now
                        "transaction_count" to txCount,
                        "avg_transaction_value" to avgTxValue
                    ),
                    actionName = "log_expense"
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

    suspend fun submitUserFeedback(predictionId: String, reward: Float) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Send to AWS
                val feedback = FeedbackRequest(predictionId, reward)
                apiService.sendFeedback(ApiClient.API_TOKEN, feedback)

                // 2. If successful, update the local database so we never send it again!
                expenseDao.markFeedbackAsSent(predictionId)

            } catch (e: Exception) {
                // If the network fails, we do nothing!
                // isFeedbackSent remains false in Room, and WorkManager will catch it later.
                println("⚠️ Feedback Sync Paused. Reason: ${e.localizedMessage}")
                println("Network failed. Feedback saved locally for eventual sync.")
            }
        }
    }
}