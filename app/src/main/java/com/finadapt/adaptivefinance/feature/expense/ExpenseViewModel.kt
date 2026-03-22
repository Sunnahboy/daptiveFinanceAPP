package com.finadapt.adaptivefinance.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. Define the exact states the UI can be in
sealed class GamificationUiState {
    object Idle: GamificationUiState()
    object Loading: GamificationUiState()
    data class Success (
        val predictionId: String,
        val message: String,
        val strategy: String,
        val action: String,
        val visualTheme: String
    ): GamificationUiState()
    data class Error(val exception: String): GamificationUiState()
}

// 🟢 FIX 1: Add the local database DAO to the constructor!
class ExpenseViewModel(
    private val repository: FinanceRepository,
    private val expenseDao: ExpenseDao,
    private val prefs: android.content.SharedPreferences,
): ViewModel() {

    private val _uiState = MutableStateFlow<GamificationUiState>(GamificationUiState.Idle)
    val uiState: StateFlow<GamificationUiState> = _uiState

    // 🟢 FIX 2: Accept the optional AI Receipt fields! (Default to empty for Manual/Voice input)
    fun submitExpense(
        amount: Float,
        category: String,
        userId: String,
        merchantName: String = "",
        date: String = "",
        paymentMethod: String = "",
        receiptImagePath: String = "",
        items: List<ReceiptItem> = emptyList()
    ) {
        if (amount <= 0.0){
            _uiState.value = GamificationUiState.Error("Please enter a valid number.")
            return
        }
        _uiState.value = GamificationUiState.Loading

        viewModelScope.launch {
            // 🟢 FIX 3: INSTANT LOCAL SAVE!
            // Save it to the phone's permanent memory immediately.
            val newExpense = ExpenseEntity(
                amount = amount,
                category = category,
                merchantName = merchantName,
                date = date,
                paymentMethod = paymentMethod,
                receiptImagePath = receiptImagePath,
                items = items
            )
            expenseDao.insertExpense(newExpense)

            // Now, ping AWS for the Gamification AI Reward
            val result = repository.logExpenseAndGetStrategy(
                userId = userId,
                amount = amount,
                category = category
            )

            result.fold(
                onSuccess = { response ->
                    val aiAction = response.action ?: "Log_Only"
                    val currentXp = prefs.getInt("USER_XP", 0)
                    val earnedXp = 50
                    val newXp = currentXp + earnedXp

                    prefs.edit {
                        putString("LAST_AI_ACTION", aiAction)
                        putBoolean("PENDING_COIN_DROP", true)
                        putInt("USER_XP", newXp)
                    }

                    _uiState.value = GamificationUiState.Success(
                        predictionId = response.predictionId ?: "unknown_id",
                        message = response.gamificationMessage ?: "Expense logged!",
                        strategy = response.recommendedStrategy ?: "Standard",
                        action = aiAction,
                        visualTheme = response.visualTheme ?: "Neutral"
                    )
                },
                onFailure = { error ->
                    _uiState.value = GamificationUiState.Error(
                        exception = error.message ?: "Unknown network error"
                    )
                }
            )
        }
    }



    // 🟢 NEW: Safely delete from the database in the background
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense.id)
        }
    }

    // 🟢 NEW: Safely update the database in the background
    fun editExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            // Because your DAO uses OnConflictStrategy.REPLACE,
            // inserting an existing ID just updates it!
            expenseDao.insertExpense(expense)
        }
    }

    fun submitFeedback(predictionId: String, strategyName: String, userAccepted: Boolean) {
        viewModelScope.launch {
            repository.submitUserFeedback(predictionId, strategyName, userAccepted)
        }
    }

    fun resetState() {
        _uiState.value = GamificationUiState.Idle
    }
}

// 🟢 FIX 4: Update the Factory so it knows how to pass the Database to the ViewModel
class ExpenseViewModelFactory(
    private val repository: FinanceRepository,
    private val expenseDao: ExpenseDao, // Added here!
    private val prefs: android.content.SharedPreferences
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, expenseDao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

