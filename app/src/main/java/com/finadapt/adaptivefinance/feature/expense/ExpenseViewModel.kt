package com.finadapt.adaptivefinance.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

//1 . Define the exact states the UI can be in

sealed class  GamificationUiState {
    object Idle: GamificationUiState()
    object  Loading: GamificationUiState()
    data class Success (
        val predictionId: String,
        val message: String,
        val strategy: String,
        val action: String,         //the Bandit's chosen action
        val visualTheme: String     //The UI color recommendation
    ): GamificationUiState()
    data class Error(val exception: String): GamificationUiState()

}

//Requires the repository via constructor
class ExpenseViewModel (private  val repository: FinanceRepository): ViewModel(){
    //2 The reactive state flow that compose will listen to
    private  val  _uiState = MutableStateFlow<GamificationUiState>(GamificationUiState.Idle)
    val uiState: StateFlow<GamificationUiState> = _uiState


    //Now accepts both the Double amount and the String category
    fun submitExpense(amount: Double, category: String,userId: String){
        if (amount <= 0.0){
            _uiState.value = GamificationUiState.Error("Please enter a valid number.")
            return
        }
        _uiState.value = GamificationUiState.Loading

        // Launch a background coroutine
        viewModelScope.launch {
            // The viewModel calls the Repository and waits
            val result = repository.logExpenseAndGetStrategy(
                userId = userId,
                amount = amount.toFloat(),
                category = category       // 🟢 NEW: Pass the category to the repository
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.value = GamificationUiState.Success(
                        predictionId = response.predictionId ?: "unknown_id",
                        message = response.gamificationMessage ?: "Expense logged!",
                        strategy = response.recommendedStrategy ?: "Standard",
                        action = response.action ?: "Log_Only",
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
    //Tells the Repository to send the +1 Reward
    fun submitFeedback(predictionId: String, reward: Float) {
        viewModelScope.launch {
            repository.submitUserFeedback(predictionId, reward)
        }
    }

    // Resets the UI so it doesn't double-fire!
    fun resetState() {
        _uiState.value = GamificationUiState.Idle
    }

}

//Tells android how to build viewModel with database
class ExpenseViewModelFactory(private val repository: FinanceRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    }

