package com.finadapt.adaptivefinance.feature.chat

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.BuildConfig
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        // 1. Get Gamification & Profile Data
        val streak = prefs.getInt("CURRENT_STREAK", 0)
        val rawName = prefs.getString("USER_NAME", "") ?: ""
        // Formats nicely so it says "Good morning, Daniel!" or just "Good morning!" if name is empty
        val userName = rawName.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""

        // 2. Get Time of Day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 5..11 -> "Good morning$userName! ☕"
            in 12..16 -> "Good afternoon$userName! ☀️"
            in 17..21 -> "Good evening$userName! 🌙"
            else -> "Late night budgeting$userName? 🦉 I respect the hustle."
        }

        // 3. Build the Contextual Message
        val streakMessage = if (streak > 2) {
            "You are on a massive $streak-day login streak! 🔥 Keep this momentum up. What are we tracking right now?"
        } else {
            "I'm your Adaptive AI coach. Ready to build some great financial habits? What's on your mind?"
        }

        // 4. Combine and display
        _messages.value = listOf(
            ChatMessage(text = "$timeGreeting $streakMessage", isFromUser = false)
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isFromUser = true)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 🟢 1. FETCH GAMIFICATION CONTEXT FIRST
                val streak = prefs.getInt("CURRENT_STREAK", 0)
                val xp = prefs.getInt("USER_XP", 0)

                // Get the very last thing the AI did to the user
                val aiEvents = expenseDao.getRecentAiInteractions()
                val lastEventString = if (aiEvents.isEmpty()) {
                    "No recent gamification interventions."
                } else {
                    val lastEvent = aiEvents.last()
                    "You recently triggered this action: [${lastEvent.action}] using Strategy: [${lastEvent.strategy}]. Message shown to user: '${lastEvent.notification}'"
                }

                // 2. Pass this tiny context to the Router!
                val jsonResponse = withContext(Dispatchers.IO) {
                    fetchIntentFromGroq(streak, xp, lastEventString)
                }

                // 3. Execute Locally
                val aiResponseText = withContext(Dispatchers.IO) {
                    executeIntentLocally(jsonResponse)
                }

                val aiMsg = ChatMessage(text = aiResponseText, isFromUser = false)
                _messages.value = _messages.value + aiMsg

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Chat Error", e)
                val errorMsg = ChatMessage(
                    text = "Sorry, my neural link dropped. (${e.localizedMessage})",
                    isFromUser = false
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * The Agentic Router
     */
    private fun fetchIntentFromGroq(streak: Int, xp: Int, lastAiEvent: String): JSONObject {
        val systemPrompt = """
            You are the routing brain for Adaptive Finance. Your ONLY job is to classify the user's intent.
            You must reply in pure JSON format. Do not include markdown formatting.
            
            USER PROFILE CONTEXT:
            - Current Streak: $streak days
            - Gamification XP: $xp
            - $lastAiEvent
            
            Valid intents:
            1. "SPEND_SUMMARY" - User asks for the TOTAL sum of money spent.
            2. "SPEND_ANALYSIS" - User asks for INSIGHTS (e.g., "Which category do I spend the most on?", "What was my highest purchase?", "Where is my money going?").
            3. "LIST_TRANSACTIONS" - User asks to SEE general items/purchases.
            4. "SEARCH_TRANSACTIONS" - User asks about a SPECIFIC item, merchant, or keyword.
            5. "BUDGET_STATUS" - User asks about remaining budget.
            6. "GENERAL_CHAT" - User says hello, asks for advice, or asks about Gamification.
            
            Rules:
            - CONTEXT INHERITANCE: If the user asks a short follow-up (e.g., "And transport?"), KEEP the exact same 'intent' and 'timeframe'.
            - 🚨 EXPLICIT OVERRIDE: If the user explicitly asks for "history", "all time", "everything", or "highest", set "timeframe" to "ALL".
            - 🧠 IMPLICIT ANALYSIS (CRITICAL): If the user asks a general trend question (e.g., "What category do I spend the most on?", "Where is my money going?") and DOES NOT specify a time, you MUST break the short-term memory context. Default the "timeframe" to "MONTH" or "ALL" so they get a meaningful answer.
            - GAMIFICATION: If the user asks about app locks or streaks, use the USER PROFILE CONTEXT to explain yourself in the 'direct_reply' field!
            
            Output Schema:
            {
              "intent": "SPEND_SUMMARY | SPEND_ANALYSIS | LIST_TRANSACTIONS | SEARCH_TRANSACTIONS | BUDGET_STATUS | GENERAL_CHAT",
              "category": "Food, Transport, etc. (Or 'ALL')",
              "timeframe": "TODAY | YESTERDAY | WEEK | MONTH | ALL",
              "search_term": "Extract specific item/merchant. Leave blank if none.",
              "direct_reply": "Only fill this out if intent is GENERAL_CHAT."
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("model", "llama-3.1-8b-instant")
            put("temperature", 0.0)
            put("response_format", JSONObject().apply { put("type", "json_object") })

            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })

                _messages.value.takeLast(5).forEach { msg ->
                    if (!msg.text.contains("Hello! I'm your Adaptive AI")) {
                        val role = if (msg.isFromUser) "user" else "assistant"
                        put(JSONObject().apply { put("role", role); put("content", msg.text) })
                    }
                }
            })
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) throw Exception("API Error ${response.code}")

        val content = JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        return JSONObject(content)
    }

    /**
     * Local Execution Engine
     */
    private suspend fun executeIntentLocally(jsonCommand: JSONObject): String {
        val intent = jsonCommand.optString("intent", "GENERAL_CHAT")
        val category = jsonCommand.optString("category", "ALL")
        val timeframe = jsonCommand.optString("timeframe", "MONTH").uppercase()

        // 🟢 1. CALENDAR MATH WITH START AND END BOUNDARIES
        var endTime = System.currentTimeMillis() // Default end time is right now
        val calendar = Calendar.getInstance()

        // Zero out the clock to exactly midnight today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime: Long = when (timeframe) {
            "TODAY" -> calendar.timeInMillis
            "YESTERDAY" -> {
                endTime = calendar.timeInMillis - 1L // End exactly at 23:59:59 yesterday
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.timeInMillis // Start exactly at 00:00:00 yesterday
            }
            "WEEK" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            "MONTH" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
            "ALL" -> 0L
            else -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
        }

        val timeLabel = when (timeframe) {
            "TODAY" -> "Today"
            "YESTERDAY" -> "Yesterday"
            "WEEK" -> "This past week"
            "MONTH" -> "This past month"
            "ALL" -> "Over all time"
            else -> "Recently"
        }

        // 🟢 2. EXECUTE QUERIES WITH BOTH START AND END TIMES
        return when (intent) {
            "SPEND_SUMMARY" -> {
                if (category == "ALL") {
                    // 1. Get the grand total
                    val totalSpent = expenseDao.getTotalSpendTimeBounded(startTime, endTime) ?: 0f
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", totalSpent)

                    if (totalSpent == 0f) {
                        "You haven't spent anything $timeLabel! Great job staying disciplined."
                    } else {
                        // 2. Get the breakdown by category
                        val breakdown = expenseDao.getCategoryBreakdown(startTime, endTime)

                        // 3. Format it into a bulleted list
                        val breakdownText = breakdown.joinToString("\n") {
                            val catTotal = String.format(Locale.getDefault(), "%.2f", it.total)
                            "- ${it.category}: RM $catTotal"
                        }

                        "$timeLabel, you've spent RM $formattedTotal total.\nHere is the breakdown:\n$breakdownText"
                    }
                } else {
                    // The user asked for a SPECIFIC category (e.g., "How much on food?")
                    val totalSpent = expenseDao.getTotalSpendByCategoryAndTime(category, startTime, endTime) ?: 0f
                    val formattedTotal = String.format(Locale.getDefault(), "%.2f", totalSpent)

                    if (totalSpent == 0f) {
                        "You haven't spent anything on $category $timeLabel."
                    } else {
                        "$timeLabel, you've spent RM $formattedTotal on $category."
                    }
                }
            }

            // 🟢 UPGRADED: The Deep Analytics Engine
            "SPEND_ANALYSIS" -> {
                val breakdown = expenseDao.getCategoryBreakdown(startTime, endTime)
                val highestSingleExpense = expenseDao.getHighestExpense(startTime, endTime)

                if (breakdown.isEmpty() || highestSingleExpense == null) {
                    "I don't have enough data to analyze your spending $timeLabel. Try logging some expenses first!"
                } else {
                    // 1. Math for the Highest Category
                    val highestCategory = breakdown.maxByOrNull { it.total }
                    val formattedCatTotal = String.format(Locale.getDefault(), "%.2f", highestCategory?.total ?: 0f)

                    // 2. Math for the Single Largest Purchase
                    val formattedSingleAmt = String.format(Locale.getDefault(), "%.2f", highestSingleExpense.amount)
                    val singleMerchant = highestSingleExpense.merchantName.ifEmpty { highestSingleExpense.category }

                    // 3. Math for the Top 3 Areas
                    val top3 = breakdown.sortedByDescending { it.total }.take(3).joinToString("\n") {
                        val catTotal = String.format(Locale.getDefault(), "%.2f", it.total)
                        "- ${it.category}: RM $catTotal"
                    }

                    // 4. The Final Masterpiece Response
                    "Here is your spending analysis $timeLabel:\n\n" +
                            "🏆 Highest Category: ${highestCategory?.category} (RM $formattedCatTotal)\n" +
                            "💸 Largest Single Purchase: RM $formattedSingleAmt at $singleMerchant\n\n" +
                            "Top spending areas:\n$top3"
                }
            }

            "LIST_TRANSACTIONS" -> {
                val recentItems = if (category == "ALL") {
                    expenseDao.getExpensesSince(startTime, endTime)
                } else {
                    expenseDao.getExpensesByCategoryAndTimeList(category, startTime, endTime)
                }

                // 🟢 Clean up the grammar so it doesn't say "your ALL purchases"
                val categoryText = if (category == "ALL") "" else " $category"

                if (recentItems.isEmpty()) {
                    "I couldn't find any specific$categoryText transactions $timeLabel."
                } else {
                    // Let's show up to 10 items if they ask for a list, 5 is too few for "all time"!
                    val formattedList = recentItems.take(10).joinToString("\n") { expense ->
                        val merchant = expense.merchantName.ifEmpty { "Unknown" }
                        val formattedAmount = String.format(Locale.getDefault(), "%.2f", expense.amount)
                        "- RM $formattedAmount at $merchant"
                    }
                    "Here are your$categoryText purchases from $timeLabel:\n$formattedList"
                }
            }


            // 🟢 NEW: The Search Engine Logic
            "SEARCH_TRANSACTIONS" -> {
                val searchTerm = jsonCommand.optString("search_term", "")

                if (searchTerm.isBlank()) {
                    "I'm not sure what specific item you are looking for. Could you clarify?"
                } else {
                    val searchResults = expenseDao.searchExpenses(searchTerm, startTime, endTime)

                    if (searchResults.isEmpty()) {
                        "I couldn't find any records matching '$searchTerm' $timeLabel."
                    } else {
                        // We will format the EXACT time (e.g., "2:30 PM") so the user knows exactly when it happened!
                        val timeFormatter = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())

                        val formattedList = searchResults.take(5).joinToString("\n") { expense ->
                            val merchant = expense.merchantName.ifEmpty { expense.category }
                            val formattedAmount = String.format(Locale.getDefault(), "%.2f", expense.amount)
                            val exactTime = timeFormatter.format(java.util.Date(expense.timestamp))

                            "- RM $formattedAmount at $merchant (Logged at $exactTime)"
                        }
                        "Here is what I found for '$searchTerm' $timeLabel:\n$formattedList"
                    }
                }
            }

            "BUDGET_STATUS" -> {
                val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
                // Updated this to pass both arguments!
                val totalSpent = expenseDao.getTotalSpendTimeBounded(thirtyDaysAgo, System.currentTimeMillis()) ?: 0f
                val remaining = budget - totalSpent

                val formattedRemaining = String.format(Locale.getDefault(), "%.2f", abs(remaining))
                val formattedBudget = String.format(Locale.getDefault(), "%.2f", budget)

                if (remaining < 0) {
                    "You are currently RM $formattedRemaining OVER your monthly budget of RM $formattedBudget! We need to activate the Aegis Vault to slow down your spending."
                } else {
                    "You have RM $formattedRemaining left in your budget for the next 30 days. You're doing great!"
                }
            }

            else -> {
                jsonCommand.optString("direct_reply", "I'm here to help you manage your finances!")
            }
        }
    }
}

// 🟢 FIX 3: Moved the Factory outside of the ViewModel class brackets!
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