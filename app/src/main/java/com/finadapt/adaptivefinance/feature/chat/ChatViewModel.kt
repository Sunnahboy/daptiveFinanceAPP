package com.finadapt.adaptivefinance.feature.chat

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.local.ChatDao
import com.finadapt.adaptivefinance.data.local.ChatMessageEntity
import com.finadapt.adaptivefinance.data.local.ChatThreadEntity
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.remote.AiIntentResponse
import com.finadapt.adaptivefinance.data.remote.ChatRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(
    private val expenseDao: ExpenseDao,
    private val chatDao: ChatDao,
    private val prefs: SharedPreferences,
    private val intentResolver: ChatIntentResolver
) : ViewModel() {

    val threads: StateFlow<List<ChatThreadEntity>> = chatDao.getAllThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentThreadId = MutableStateFlow<String?>(null)
    val currentThreadId: StateFlow<String?> = _currentThreadId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val greetingText: String
        get() {
            val userName = prefs.getString("USER_NAME", "")?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
            return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Good morning$userName."
                in 12..16 -> "Good afternoon$userName."
                in 17..21 -> "Good evening$userName."
                else -> "Late night budgeting$userName?"
            }
        }

    init {
        viewModelScope.launch {
            ensureActiveThread()
        }
    }

    private fun ensureActiveThread() {
        createNewChat()
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newThreadId = UUID.randomUUID().toString()
            val thread = ChatThreadEntity(
                threadId = newThreadId,
                title = "New Chat",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isPinned = false
            )
            withContext(Dispatchers.IO) {
                chatDao.insertThread(thread)
            }
            _currentThreadId.value = newThreadId
            _messages.value = emptyList()
        }
    }

    fun selectThread(threadId: String) {
        _currentThreadId.value = threadId
        viewModelScope.launch {
            val messageEntities = withContext(Dispatchers.IO) {
                chatDao.getMessagesForThread(threadId)
            }
            val filtered = messageEntities.filter { entity ->
                !entity.isFromUser && (
                    entity.text.contains("Adaptive AI coach") ||
                    entity.text.contains("Good morning") ||
                    entity.text.contains("Good afternoon") ||
                    entity.text.contains("Good evening")
                ).not()
            }
            _messages.value = filtered.map {
                ChatMessage(id = it.messageId, text = it.text, isFromUser = it.isFromUser, timestamp = it.timestamp)
            }
        }
    }

    fun renameThread(threadId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.updateThreadTitle(threadId, newTitle.trim())
            }
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.deleteMessagesForThread(threadId)
                chatDao.deleteThread(threadId)
            }
            if (_currentThreadId.value == threadId) {
                // Select another thread or create new
                val remainingThreads = threads.value.filter { it.threadId != threadId }
                if (remainingThreads.isNotEmpty()) {
                    selectThread(remainingThreads.first().threadId)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun togglePinThread(threadId: String, isPinned: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                chatDao.updateThreadPinned(threadId, isPinned)
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val threadId = _currentThreadId.value ?: return

        val userMsgId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(id = userMsgId, text = userText, isFromUser = true)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            // Save user message to DB
            val userMsgEntity = ChatMessageEntity(
                messageId = userMsgId,
                threadId = threadId,
                text = userText,
                isFromUser = true,
                timestamp = userMsg.timestamp
            )
            withContext(Dispatchers.IO) {
                chatDao.insertMessage(userMsgEntity)
                // If thread title is still "New Chat", update it to the first few words of the user prompt
                // We can check or update title
                chatDao.updateThreadTimestamp(threadId)
            }

            // Update title if it's "New Chat"
            val currentThread = threads.value.find { it.threadId == threadId }
            if (currentThread?.title == "New Chat") {
                val newTitle = if (userText.length > 25) userText.take(22) + "..." else userText
                renameThread(threadId, newTitle)
            }

            try {
                val userId = prefs.getString("USER_ID", "default_user") ?: "default_user"
                val streak = prefs.getInt("CURRENT_STREAK", 0)
                val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                val currencySymbol = prefs.getString("PREFERRED_CURRENCY", "RM") ?: "RM"

                val calendar = Calendar.getInstance()
                val todayStart = calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
                val monthStart = calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis

                val (spentToday, spentMonth) = withContext(Dispatchers.IO) {
                    val today = expenseDao.getTotalSpendTimeBounded(todayStart, System.currentTimeMillis()) ?: 0f
                    val month = expenseDao.getTotalSpendTimeBounded(monthStart, System.currentTimeMillis()) ?: 0f
                    Pair(today, month)
                }

                val aiCommand = withContext(Dispatchers.IO) {
                    fetchIntentFromBackend(userId, userText, streak, (budget - spentMonth), spentToday, currencySymbol)
                }

                val aiIntro = aiCommand.directReply
                val localData = withContext(Dispatchers.IO) {
                    intentResolver.execute(aiCommand)
                }

                val finalResponseText = when {
                    aiIntro.isNotBlank() && localData.isNotBlank() -> "$aiIntro\n\n$localData"
                    aiIntro.isNotBlank() -> aiIntro
                    localData.isNotBlank() -> localData
                    else -> "I processed that, but I'm not sure what to show you!"
                }

                val aiMsgId = UUID.randomUUID().toString()
                val aiMsg = ChatMessage(id = aiMsgId, text = finalResponseText, isFromUser = false)
                _messages.value = _messages.value + aiMsg

                val aiMsgEntity = ChatMessageEntity(
                    messageId = aiMsgId,
                    threadId = threadId,
                    text = finalResponseText,
                    isFromUser = false,
                    timestamp = aiMsg.timestamp
                )
                withContext(Dispatchers.IO) {
                    chatDao.insertMessage(aiMsgEntity)
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI Chat Error", e)
                val errorText = "My servers are catching their breath. Try again in a sec!"
                val errorMsgId = UUID.randomUUID().toString()
                _messages.value = _messages.value + ChatMessage(id = errorMsgId, text = errorText, isFromUser = false)
                val errorEntity = ChatMessageEntity(
                    messageId = errorMsgId,
                    threadId = threadId,
                    text = errorText,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    chatDao.insertMessage(errorEntity)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchIntentFromBackend(
        userId: String, message: String, streak: Int, remainingBudget: Float, spentToday: Float, currencySymbol: String
    ): AiIntentResponse {

        val recentHistory = _messages.value.takeLast(4).joinToString("\n") {
            if (it.isFromUser) "User: ${it.text}" else "AI: ${it.text.take(50)}..."
        }

        val promptContext = """
            [SYSTEM VITALS] Streak: $streak days | Currency Symbol: $currencySymbol | Budget Remaining: $currencySymbol $remainingBudget | Spent Today: $currencySymbol $spentToday
            [CURRENCY RULE] Strictly use the user's preferred currency symbol/code '$currencySymbol' (e.g. '$currencySymbol 50') for all monetary amounts in your direct reply. Never use colloquial terms like 'riggit' or incorrect spellings.
            [EMOJI RULE] Do not include star, sparkle, or similar glowing/star emojis (like ❇️, ✨, ⭐) in your responses.
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

class ChatViewModelFactory(
    private val expenseDao: ExpenseDao,
    private val chatDao: ChatDao,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val resolver = ChatIntentResolver(expenseDao, prefs)
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(expenseDao, chatDao, prefs, resolver) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
