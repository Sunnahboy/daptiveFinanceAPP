package com.finadapt.adaptivefinance.feature.chat

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.remote.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs


data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val streak = prefs.getInt("CURRENT_STREAK", 0)
        val userName = prefs.getString("USER_NAME", "")?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""

        val timeGreeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning$userName! ☕"
            in 12..16 -> "Good afternoon$userName! ☀️"
            in 17..21 -> "Good evening$userName! 🌙"
            else -> "Late night budgeting$userName? 🦉 I respect the hustle."
        }

        val streakMessage = if (streak > 2) {
            "You are on a massive $streak-day login streak! 🔥 Keep this momentum up. What are we tracking right now?"
        } else {
            "I'm your Adaptive AI coach. Ready to build some great financial habits? What's on your mind?"
        }

        _messages.value = listOf(ChatMessage(text = "$timeGreeting $streakMessage", isFromUser = false))
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        _messages.value = _messages.value + ChatMessage(text = userText, isFromUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Gather minimal context
                val streak = prefs.getInt("CURRENT_STREAK", 0)
                val xp = prefs.getInt("USER_XP", 0)
                val lastEvent = expenseDao.getRecentAiInteractions().lastOrNull()?.action ?: "None"
                val userId = prefs.getString("USER_ID", "default_user") ?: "default_user"

                // 2. Fetch from Ktor Backend via Retrofit
                val intentJson = withContext(Dispatchers.IO) {
                    fetchIntentFromBackend(userId, userText, streak, xp, lastEvent)
                }

                // 3. Execute locally using the DB
                val aiResponseText = withContext(Dispatchers.IO) {
                    executeIntentLocally(intentJson)
                }

                _messages.value = _messages.value + ChatMessage(text = aiResponseText, isFromUser = false)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Chat Error", e)
                _messages.value = _messages.value + ChatMessage(
                    text = "Sorry, my neural link dropped. (${e.localizedMessage})",
                    isFromUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    //DSL-LIKE NETWORK CALL
    private suspend fun fetchIntentFromBackend(userId: String, message: String, streak: Int, xp: Int, lastEvent: String): JSONObject {
        // We bundle the user's message with their current context to send to Ktor
        val promptContext = "Context: Streak=$streak, XP=$xp, LastAction=$lastEvent. User says: $message"
        val request = ChatRequest(prompt = promptContext)

        val response = ApiClient.rateLimitApiService.askFinancialAi(userId, request)

        return when {
            response.isSuccessful && response.body() != null -> {
                // The backend will return a JSON string, we parse it here so executeIntentLocally can read it
                JSONObject(response.body()!!.message)
            }
            response.code() == 429 -> throw Exception("Rate limit exceeded. Please wait a moment.")
            else -> throw Exception("Backend error ${response.code()}")
        }
    }

    //LOCAL DB EXECUTION
    private suspend fun executeIntentLocally(jsonCommand: JSONObject): String {
        val intent = jsonCommand.optString("intent", "GENERAL_CHAT")
        val category = jsonCommand.optString("category", "ALL")
        val timeframe = jsonCommand.optString("timeframe", "MONTH").uppercase()

        var endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val startTime: Long = when (timeframe) {
            "TODAY" -> calendar.timeInMillis
            "YESTERDAY" -> {
                endTime = calendar.timeInMillis - 1L
                calendar.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
            }
            "WEEK" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            "MONTH" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            "ALL" -> 0L
            else -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        }

        val timeLabel = when (timeframe) {
            "TODAY" -> "Today"
            "YESTERDAY" -> "Yesterday"
            "WEEK" -> "This past week"
            "MONTH" -> "This past month"
            "ALL" -> "Over all time"
            else -> "Recently"
        }

        return when (intent) {
            "SPEND_SUMMARY" -> {
                if (category == "ALL") {
                    val totalSpent = expenseDao.getTotalSpendTimeBounded(startTime, endTime) ?: 0f
                    if (totalSpent == 0f) return "You haven't spent anything $timeLabel! Great job staying disciplined."

                    val breakdownText = expenseDao.getCategoryBreakdown(startTime, endTime).joinToString("\n") {
                        "- ${it.category}: RM ${String.format(Locale.getDefault(), "%.2f", it.total)}"
                    }
                    "$timeLabel, you've spent RM ${String.format(Locale.getDefault(), "%.2f", totalSpent)} total.\nHere is the breakdown:\n$breakdownText"
                } else {
                    val totalSpent = expenseDao.getTotalSpendByCategoryAndTime(category, startTime, endTime) ?: 0f
                    if (totalSpent == 0f) "You haven't spent anything on $category $timeLabel."
                    else "$timeLabel, you've spent RM ${String.format(Locale.getDefault(), "%.2f", totalSpent)} on $category."
                }
            }
            "SPEND_ANALYSIS" -> {
                val breakdown = expenseDao.getCategoryBreakdown(startTime, endTime)
                val highestSingleExpense = expenseDao.getHighestExpense(startTime, endTime)

                if (breakdown.isEmpty() || highestSingleExpense == null) return "I don't have enough data to analyze your spending $timeLabel. Try logging some expenses first!"

                val highestCategory = breakdown.maxByOrNull { it.total }
                val singleMerchant = highestSingleExpense.merchantName.ifEmpty { highestSingleExpense.category }
                val top3 = breakdown.sortedByDescending { it.total }.take(3).joinToString("\n") {
                    "- ${it.category}: RM ${String.format(Locale.getDefault(), "%.2f", it.total)}"
                }

                "Here is your spending analysis $timeLabel:\n\n" +
                        "🏆 Highest Category: ${highestCategory?.category} (RM ${String.format(Locale.getDefault(), "%.2f", highestCategory?.total ?: 0f)})\n" +
                        "💸 Largest Single Purchase: RM ${String.format(Locale.getDefault(), "%.2f", highestSingleExpense.amount)} at $singleMerchant\n\n" +
                        "Top spending areas:\n$top3"
            }
            "LIST_TRANSACTIONS" -> {
                val recentItems = if (category == "ALL") expenseDao.getExpensesSince(startTime, endTime) else expenseDao.getExpensesByCategoryAndTimeList(category, startTime, endTime)
                val categoryText = if (category == "ALL") "" else " $category"

                if (recentItems.isEmpty()) return "I couldn't find any specific$categoryText transactions $timeLabel."

                val formattedList = recentItems.take(10).joinToString("\n") {
                    "- RM ${String.format(Locale.getDefault(), "%.2f", it.amount)} at ${it.merchantName.ifEmpty { "Unknown" }}"
                }
                "Here are your$categoryText purchases from $timeLabel:\n$formattedList"
            }
            "SEARCH_TRANSACTIONS" -> {
                val searchTerm = jsonCommand.optString("search_term", "")
                if (searchTerm.isBlank()) return "I'm not sure what specific item you are looking for. Could you clarify?"

                val searchResults = expenseDao.searchExpenses(searchTerm, startTime, endTime)
                if (searchResults.isEmpty()) return "I couldn't find any records matching '$searchTerm' $timeLabel."

                val timeFormatter = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
                val formattedList = searchResults.take(5).joinToString("\n") {
                    "- RM ${String.format(Locale.getDefault(), "%.2f", it.amount)} at ${it.merchantName.ifEmpty { it.category }} (Logged at ${timeFormatter.format(java.util.Date(it.timestamp))})"
                }
                "Here is what I found for '$searchTerm' $timeLabel:\n$formattedList"
            }
            "BUDGET_STATUS" -> {
                val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
                val totalSpent = expenseDao.getTotalSpendTimeBounded(thirtyDaysAgo, System.currentTimeMillis()) ?: 0f
                val remaining = budget - totalSpent

                if (remaining < 0) "You are currently RM ${String.format(Locale.getDefault(), "%.2f", abs(remaining))} OVER your monthly budget! We need to activate the Aegis Vault."
                else "You have RM ${String.format(Locale.getDefault(), "%.2f", remaining)} left in your budget for the next 30 days. You're doing great!"
            }
            else -> jsonCommand.optString("direct_reply", "I'm here to help you manage your finances!")
        }
    }
}

class ChatViewModelFactory(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(expenseDao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}