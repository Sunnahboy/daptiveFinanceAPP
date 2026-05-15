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
import java.util.Calendar
import com.finadapt.adaptivefinance.data.remote.AiIntentResponse
import com.google.gson.Gson



data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences,
    private val intentResolver: ChatIntentResolver
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
            "You're on a $streak-day tracking streak! 🔥 Let's keep your wallet happy."
        } else {
            "I'm your Adaptive AI coach. What's on your mind today?"
        }

        _messages.value = listOf(ChatMessage(text = "$timeGreeting $streakMessage", isFromUser = false))
    }



    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        _messages.value = _messages.value + ChatMessage(text = userText, isFromUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val userId = prefs.getString("USER_ID", "default_user") ?: "default_user"
                val streak = prefs.getInt("CURRENT_STREAK", 0)
                val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)

                val calendar = Calendar.getInstance()
                val todayStart = calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
                val monthStart = calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis

                val (spentToday, spentMonth) = withContext(Dispatchers.IO) {
                    val today = expenseDao.getTotalSpendTimeBounded(todayStart, System.currentTimeMillis()) ?: 0f
                    val month = expenseDao.getTotalSpendTimeBounded(monthStart, System.currentTimeMillis()) ?: 0f
                    Pair(today, month)
                }

                // 1. Fetch AI Response Model safely
                val aiCommand = withContext(Dispatchers.IO) {
                    fetchIntentFromBackend(userId, userText, streak, (budget - spentMonth), spentToday)
                }

                // 2. Delegate logic to the Domain Resolver
                val aiIntro = aiCommand.directReply
                val localData = withContext(Dispatchers.IO) {
                    intentResolver.execute(aiCommand)
                }

                val finalMessage = when {
                    aiIntro.isNotBlank() && localData.isNotBlank() -> "$aiIntro\n\n$localData"
                    aiIntro.isNotBlank() -> aiIntro
                    localData.isNotBlank() -> localData
                    else -> "I processed that, but I'm not sure what to show you!"
                }

                _messages.value = _messages.value + ChatMessage(text = finalMessage, isFromUser = false)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Chat Error", e)
                _messages.value = _messages.value + ChatMessage(text = "My servers are catching their breath. Try again in a sec!", isFromUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchIntentFromBackend(
        userId: String, message: String, streak: Int, remainingBudget: Float, spentToday: Float
    ): AiIntentResponse {

        val recentHistory = _messages.value.takeLast(4).joinToString("\n") {
            if (it.isFromUser) "User: ${it.text}" else "AI: ${it.text.take(50)}..."
        }

        val promptContext = """
            [SYSTEM VITALS] Streak: $streak days | Budget Remaining: RM $remainingBudget | Spent Today: RM $spentToday
            [CHAT HISTORY]
            $recentHistory
            [NEW MESSAGE]
            $message
        """.trimIndent()

        val response = ApiClient.rateLimitApiService.askFinancialAi(userId, ChatRequest(promptContext))

        return when {
            response.isSuccessful && response.body() != null -> {
                Gson().fromJson(response.body()!!.message, AiIntentResponse::class.java)
            }
            response.code() == 429 -> throw Exception("Rate limit exceeded.")
            else -> throw Exception("Backend error ${response.code()}")
        }
    }
}

//Update Factory to provide the Resolver
class ChatViewModelFactory(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val resolver = ChatIntentResolver(expenseDao, prefs)
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(expenseDao, prefs, resolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}