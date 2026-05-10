package com.finadapt.adaptivefinance.data.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.local.AiInteractionEntity
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.remote.AiResponse
import com.finadapt.adaptivefinance.data.remote.ContextRequest
import com.finadapt.adaptivefinance.data.remote.FeedbackRequest
import com.finadapt.adaptivefinance.data.remote.LeaderboardEntry
import com.finadapt.adaptivefinance.data.remote.LeaderboardUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Calendar.getInstance
import kotlin.math.ln
import kotlin.math.sqrt
import com.finadapt.adaptivefinance.data.remote.CheerRequest

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) {

    private val apiService = ApiClient.fastApiService

    // "fire-and-forget" background tasks
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    suspend fun logExpenseAndGetStrategy(
        userId: String,
        amount: Float,
        category: String = "General"
    ): Result<AiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. STREAK MANAGEMENT
                val todayMidnight = getMidnightTimestamp()
                val lastLoggedMidnight = prefs.getLong("LAST_LOGGED_MIDNIGHT", 0L)
                var currentStreak = prefs.getInt("CURRENT_STREAK", 0)
                val daysDifference = (todayMidnight - lastLoggedMidnight) / (1000 * 60 * 60 * 24)

                when (daysDifference) {
                    0L -> { /* Already logged today */ }
                    1L -> { currentStreak += 1 }
                    else -> { currentStreak = 1 }
                }
                prefs.edit {
                    putLong("LAST_LOGGED_MIDNIGHT", todayMidnight)
                    putInt("CURRENT_STREAK", currentStreak)
                }


                // 2. FETCH RAW DATA FROM DAO (Strict 30-Day Rolling Window)
                val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
                val currentCycleStart = System.currentTimeMillis() - thirtyDaysInMillis

                // Only grab the total spend from THIS cycle, ignoring lifetime spend
                val totalSpend = expenseDao.getTotalSpendTimeBounded(currentCycleStart, System.currentTimeMillis()) ?: 0f
                val txCount = expenseDao.getTransactionCount().toFloat()

                // 3. EDGE FEATURE ENGINEERING (Cycle-Based Pace System)
                val monthlyBudget = prefs.getFloat("MONTHLY_BUDGET", 1000f)

                // Figure out exactly what day we are on in the 30-day cycle
                val installTimestamp = prefs.getLong("INSTALL_TIMESTAMP", System.currentTimeMillis())
                val millisActive = System.currentTimeMillis() - installTimestamp
                val totalDaysActive = millisActive / (1000f * 60f * 60f * 24f)
                val daysIntoCurrentCycle = (totalDaysActive % 30f).coerceAtLeast(1f)
                val daysRemaining = (30f - daysIntoCurrentCycle).coerceAtLeast(1f)

                val remainingBudget = maxOf(0f, monthlyBudget - totalSpend)

                //Floor-Bounded Dynamic Allowance
                val baseDailyAllowance = if (monthlyBudget > 0f) monthlyBudget / 30f else 50f
                val strictRollingAllowance = remainingBudget / daysRemaining

                //The allowance adjusts down, but never drops below 40%
                // of their base allowance, unless their remaining budget is completely gone.
                val safeDailyAllowance = if (remainingBudget > 0) {
                    maxOf(strictRollingAllowance, baseDailyAllowance * 0.40f)
                } else {
                    10f //Absolute floor so the natural log math below doesn't explode
                }

                // PACE CALCULATION (Macro Drift)
                // Are they burning budget too fast relative to what day of the cycle it is?
                val expectedSpendByToday = baseDailyAllowance * daysIntoCurrentCycle
                val macroDrift = if (expectedSpendByToday > 0f) (totalSpend / expectedSpendByToday) else 0.5f

                // VOLATILITY CALCULATION (Micro Anomaly)
                val transactionSpikeRatio = amount / safeDailyAllowance
                val microAnomaly = ln(1.0 + transactionSpikeRatio).toFloat()

                val synthesizedRisk = sqrt((macroDrift * macroDrift) + (microAnomaly * microAnomaly))
                val finalVolatility = synthesizedRisk.coerceIn(0.0f, 5.0f)

                val pastTotalSpend = maxOf(0f, totalSpend - amount)
                val pastTxCount = maxOf(1f, txCount - 1f)
                val avgTxValue = if (pastTxCount > 0f) pastTotalSpend / pastTxCount else 0f

                // The impulse modifier (scales to 1.0)
                val calculatedImpulse = (transactionSpikeRatio * 0.10f).coerceIn(0.0f, 1.0f)

                // 4. PREPARE PAYLOAD
                val request = ContextRequest(
                    userId = userId,
                    testGroup = "adaptive",
                    amount = amount,
                    category = category,
                    features = mapOf(
                        "total_spend" to totalSpend,
                        "spending_volatility" to finalVolatility,
                        "return_rate" to calculatedImpulse,
                        "transaction_count" to txCount,
                        "avg_transaction_value" to avgTxValue
                    )
                )
                //Logs fo debugging
                Log.d("MATH_CHECK", "App is sending Volatility: $finalVolatility | Return Rate (Impulse): $calculatedImpulse")

                // 5. SEND TO server
                val response = try {
                    apiService.getAiGamification(ApiClient.API_TOKEN, request)
                } catch (_: Exception) {
                    Log.w("Gamification", "User is offline. Using local fallback.")

                    // If there is no internet, we default to skipping the game
                    AiResponse(
                        predictionId = "offline_${java.util.UUID.randomUUID()}",
                        recommendedStrategy = "Standard",
                        action = "Log_Only",
                        gamificationMessage = "Expense logged securely offline.",
                        visualTheme = "Neutral"
                    )
                }

                //START OF bandit GUARDRAIL
                var finalResponse = response

                if (finalVolatility >= 4.0f && response.action?.contains("Streak", ignoreCase = true) == true) {
                    Log.w("Gamification", "🛡️ GUARDRAIL TRIGGERED: Overriding AI during critical overspend.")

                    val badPredictionId = response.predictionId
                    if (badPredictionId != null) {
                        repositoryScope.launch {
                            try {
                                val punishFeedback = FeedbackRequest(badPredictionId, -5.0f)
                                apiService.sendFeedback(ApiClient.API_TOKEN, punishFeedback)
                            } catch (e: Exception) {
                                Log.e("Gamification", "Failed to send auto-punish feedback", e)
                            }
                        }
                    }

                    finalResponse = response.copy(
                        predictionId = "local_override_${java.util.UUID.randomUUID()}",
                        action = "strict_budget", // Ensured exact match with your game constants
                        recommendedStrategy = "Strict_Budget",
                        gamificationMessage = "CRITICAL OVERSPEND. Aegis Protocol initiated. Secure the vault.",
                        visualTheme = "Danger"
                    )
                }
                // END OF bandit  GUARDRAIL

                // 6. SAVE INTERACTION TO MEMORY
                val currentPredictionId = finalResponse.predictionId ?: "fallback_${java.util.UUID.randomUUID()}"
                val aiMemory = AiInteractionEntity(
                    predictionId = currentPredictionId,
                    strategy = finalResponse.recommendedStrategy ?: "Standard",
                    action = finalResponse.action ?: "Log_Only",
                    notification = finalResponse.gamificationMessage ?: "Expense logged.",
                    visualTheme = finalResponse.visualTheme ?: "Neutral"
                )
                expenseDao.insertAiInteraction(aiMemory)

                Result.success(finalResponse)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitUserFeedback(predictionId: String, strategyName: String, userAccepted: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val strikeKey = "STRIKES_$strategyName"
                val finalReward: Float

                if (userAccepted) {
                    //THEY ACCEPTED
                    finalReward = 2.0f
                    prefs.edit { putInt(strikeKey, 0) }

                    val currentXp = prefs.getInt("USER_XP", 0)
                    prefs.edit { putInt("USER_XP", currentXp + 50) }

                } else {
                    //THEY DECLINED
                    var currentStrikes = prefs.getInt(strikeKey, 0)
                    currentStrikes += 1

                    if (currentStrikes >= 3) {
                        //3 STRIKES: Massive punishment to force the Bandit to switch arms
                        finalReward = -5.0f
                        prefs.edit { putInt(strikeKey, 0) }
                    } else {
                        //SOFT IGNORE: A slight negative nudge so it starts losing confidence
                        finalReward = -0.5f
                        prefs.edit { putInt(strikeKey, currentStrikes) }
                    }
                }

                val feedback = FeedbackRequest(predictionId, finalReward)
                apiService.sendFeedback(ApiClient.API_TOKEN, feedback)
                expenseDao.markFeedbackAsSent(predictionId)

            } catch (e: Exception) {
                println(" Feedback Sync Paused. Reason: ${e.localizedMessage}")
            }
        }
    }


    //Helper: Gets the exact millisecond a day started
    private fun getMidnightTimestamp(timeInMillis: Long = System.currentTimeMillis()): Long {
        val cal = getInstance().apply { this.timeInMillis = timeInMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    //Read the Streak safely for the UI
    fun getLiveStreak(): Int {
        val todayMidnight = getMidnightTimestamp()
        val lastLoggedMidnight = prefs.getLong("LAST_LOGGED_MIDNIGHT", 0L)
        val currentStreak = prefs.getInt("CURRENT_STREAK", 0)

        // Calculate how many days it has been since their last log
        val daysDifference = (todayMidnight - lastLoggedMidnight) / (1000 * 60 * 60 * 24)

        return if (daysDifference <= 1L) {
            //They logged today (0) or yesterday (1). The streak is alive!
            currentStreak
        } else {
            // It has been 2 or more days. The streak is broken!
            0
        }
    }

    //Read the last seen tier
    fun getLastSeenTier(): String {
        return prefs.getString("LAST_SEEN_TIER", "Bronze Novice") ?: "Bronze Novice"
    }

    //Save the new tier
    fun saveLastSeenTier(tierName: String) {
        prefs.edit { putString("LAST_SEEN_TIER", tierName) }
    }

    //Save the Dark Mode choice
    fun saveThemePreference(isDark: Boolean) {
        prefs.edit { putBoolean("IS_DARK_MODE", isDark) }
    }

    //Read the Dark Mode choice (defaults to false/Light Mode)
    fun isDarkMode(): Boolean {
        return prefs.getBoolean("IS_DARK_MODE", false)
    }

    //Generate and stores a secure anonymous Name
    fun getAnonymousName(): String {
        var name = prefs.getString("ANONYMOUS_NAME", null)
        if (name == null) {
            name = "Saver_${(1000..9999).random()}"
            prefs.edit { putString("ANONYMOUS_NAME", name) }
        }
        return name
    }


    // silently sync users Xp to the supabase
    suspend fun syncLeaderboard(xp: Int, tier: String){
        try{
            val currentUserId = prefs.getString("SILENT_USER_ID", "fallback_id") ?: "fallback_id"
            val anonName = getAnonymousName()

            val request = LeaderboardUpdateRequest(
                userId = currentUserId,
                anonymousName = anonName,
                xp = xp,
                tier = tier
            )
            ApiClient.fastApiService.syncLeaderboardXp(request)

        }catch (e: Exception){
            Log.e("TAG", "Failed to process data", e)
        }
    }

    // 🟢 PASTE THE NEW FEATURES RIGHT HERE!

    // 1. The "Live" Stream (Polls every 5 seconds for the Community UI)
    fun getLiveLeaderboardStream(): kotlinx.coroutines.flow.Flow<List<LeaderboardEntry>> = kotlinx.coroutines.flow.flow {
        while (true) {
            try {
                val response = ApiClient.fastApiService.getLeaderboardTop(ApiClient.API_TOKEN)
                if (response.isSuccessful) {
                    emit(response.body()?.data ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("LeaderboardStream", "Fetch failed in stream", e)
            }
            kotlinx.coroutines.delay(5000) // Wait 5 seconds, then fetch again!
        }
    }

    suspend fun getHallOfFame(): List<LeaderboardEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.fastApiService.getLeaderboardHistory(ApiClient.API_TOKEN)
                if (response.isSuccessful) {
                    // Map HallOfFameEntry to LeaderboardEntry to resolve type mismatch
                    response.body()?.data?.map { historyItem ->
                        LeaderboardEntry(
                            userId = "", // Hall of Fame entries don't have a userId
                            anonymousName = historyItem.anonymousName,
                            xp = historyItem.xp,
                            tier = historyItem.tier
                        )
                    } ?: emptyList()
                } else {
                    Log.e("HallOfFame", "Server error: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("HallOfFame", "Failed to fetch Hall of Fame: ${e.message}")
                emptyList()
            }
        }
    }

    // 2. The Coin Deductor (For when they click the "Send Cheer" button)
    fun deductCoins(amount: Int): Boolean {
        // Since spendable coins = (USER_XP - SPENT_COINS),
        // to deduct coins, we just add to the SPENT_COINS tally!
        val currentSpent = prefs.getInt("SPENT_COINS", 0)
        prefs.edit { putInt("SPENT_COINS", currentSpent + amount) }
        return true
    }

    // 3. The Cheer API Call
    suspend fun sendAnonymousCheer(targetUserId: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = CheerRequest(targetUserId = targetUserId)
                apiService.sendCheer(ApiClient.API_TOKEN, request)
                Log.d("CommunityRepo", "Cheer successfully sent for user: $targetUserId")
            } catch (e: Exception) {
                Log.e("CommunityRepo", "Failed to send cheer: ${e.message}")
            }
        }
    }

    fun getSpendableCoins(): Int {
        val totalXp = prefs.getInt("USER_XP", 0)
        val spentCoins = prefs.getInt("SPENT_COINS", 0)
        return (totalXp - spentCoins).coerceAtLeast(0)
    }
}
