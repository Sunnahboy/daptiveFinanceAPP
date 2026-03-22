

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 1. Data class for our Chat Bubbles
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
        _messages.value = listOf(
            ChatMessage(text = "Hello! I'm your Adaptive AI. Ask me anything about your recent spending, budget, or streaks.", isFromUser = false)
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isFromUser = true)
        val currentHistory = _messages.value + userMsg
        _messages.value = currentHistory
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val aiResponseText = withContext(Dispatchers.IO) {

                    // --- A. Database Retrieval (Expenses) ---
                    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                    val recentExpenses = expenseDao.getExpensesSince(thirtyDaysAgo).take(100)

                    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val contextDataString = if (recentExpenses.isEmpty()) {
                        "No transactions found."
                    } else {
                        recentExpenses.joinToString("\n") { exp ->
                            val date = dateFormatter.format(Date(exp.timestamp))
                            val merchant = exp.merchantName.ifEmpty { "Unknown Merchant" }
                            "- $date: RM ${exp.amount} on ${exp.category} ($merchant)"
                        }
                    }

                    // --- B. Preferences Retrieval (Profile & Budget) ---
                    val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                    val userName = prefs.getString("USER_NAME", "User") ?: "User"
                    val streak = prefs.getInt("CURRENT_STREAK", 0)
                    val xp = prefs.getInt("USER_XP", 0)

                    val userProfileString = """
                        Name: $userName
                        Monthly Budget: RM $budget
                        Current Login Streak: $streak days
                        Gamification XP: $xp
                    """.trimIndent()

                    // --- C. Gamification Retrieval (Bandit AI Memory) ---
                    val recentAiEvents = expenseDao.getRecentAiInteractions().takeLast(5)
                    val aiEventsString = if (recentAiEvents.isEmpty()) {
                        "No recent gamification interventions."
                    } else {
                        recentAiEvents.joinToString("\n") { event ->
                            "- Triggered Action: [${event.action}] using Strategy: [${event.strategy}]. Message shown to user: '${event.notification}'"
                        }
                    }

                    // Pass all THREE strings to Groq!
                    fetchGroqResponse(currentHistory, contextDataString, userProfileString, aiEventsString)
                }

                val aiMsg = ChatMessage(text = aiResponseText, isFromUser = false)
                _messages.value = _messages.value + aiMsg

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Chat Error", e)
                val errorMsg = ChatMessage(text = "Sorry, I'm having trouble connecting to my brain right now. (${e.localizedMessage})", isFromUser = false)
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    // THE ONLY fetchGroqResponse FUNCTION YOU NEED
    private suspend fun fetchGroqResponse(
        chatHistory: List<ChatMessage>,
        transactionData: String,
        userProfile: String,
        gamificationHistory: String
    ): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = """
                You are a helpful, concise financial assistant inside a mobile app.
                You are the "brain" of the app. You sometimes trigger gamification mini-games to help the user save money.
                
                USER PROFILE:
                ---
                $userProfile
                ---
                
                USER'S RECENT TRANSACTIONS (Last 30 days):
                ---
                $transactionData
                ---
                
                YOUR RECENT INTERVENTIONS (Gamification triggered by you):
                ---
                $gamificationHistory
                ---
                
                RULES:
                1. ONLY answer based on the transaction, profile, and gamification data provided above.
                2. Do not hallucinate or guess numbers. If the data isn't there, say you don't know.
                3. Keep your answers short, friendly, and formatted nicely (use bullet points if needed).
                4. Calculate totals accurately if the user asks.
                5. If the user asks why you locked their app or triggered a game, look at "YOUR RECENT INTERVENTIONS" to explain yourself.
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("temperature", 0.1)

                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })

                    chatHistory.filter { !it.text.contains("Hello! I'm your Adaptive AI") }
                        .takeLast(6)
                        .forEach { msg ->
                            val role = if (msg.isFromUser) "user" else "assistant"
                            put(JSONObject().apply { put("role", role); put("content", msg.text) })
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

            val rootJson = JSONObject(responseBody)
            rootJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
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