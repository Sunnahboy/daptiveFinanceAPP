
package com.finadapt.adaptivefinance.feature.expense

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: FinanceRepository,
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences,
): ViewModel() {

    // 🟢 1. THE QUEUE MANAGER
    // This holds our countdown timer so we can cancel it if they log rapidly.
    private var aiSyncJob: Job? = null
    //holds the total amount and categories of rapid fire logging
    private var accumulatedAmount = 0f
    private var lastCategory = ""

    fun logExpense(
        amount: Float,
        category: String,
        merchantName: String = "",
        date: String = "",
        paymentMethod: String = "",
        receiptImagePath: String = "",
        items: List<ReceiptItem> = emptyList()
    ) {
        if (amount <= 0.0f) return

        // --- STEP 1: INSTANT LOCAL SAVE & REWARD ---
        // We launch this instantly. No delays.
        viewModelScope.launch {
            val newExpense = ExpenseEntity(
                id = 0, // Room will auto-generate the actual ID
                amount = amount,
                category = category,
                merchantName = merchantName,
                date = date,
                paymentMethod = paymentMethod,
                receiptImagePath = receiptImagePath,
                items = items,
                timestamp = System.currentTimeMillis()
            )
            expenseDao.insertExpense(newExpense)

            // Instant reward (Fix: We only do this once now!)
            val currentXp = prefs.getInt("USER_XP", 0)
            prefs.edit {
                putInt("USER_XP", currentXp + 50)
                putBoolean("PENDING_COIN_DROP", true)
            }
        }

        //add to the accumulator
        accumulatedAmount += amount
        lastCategory = category

        // --- STEP 2: THE DEBOUNCER ---
        // Cancel the previous countdown if they log another expense quickly
        aiSyncJob?.cancel()

        // Start a new countdown
        aiSyncJob = viewModelScope.launch {
            // Wait 2 seconds to see if they log another expense
            delay(2000)
            //capture the combined total
            val amountToSend = accumulatedAmount
            val categoryToSend = lastCategory
            accumulatedAmount = 0f

            // --- STEP 3: SILENT BACKGROUND AI CHECK ---
            val userId = prefs.getString("SILENT_USER_ID", "default_user") ?: "default_user"
            val result = repository.logExpenseAndGetStrategy(userId, amountToSend, categoryToSend)

            result.fold(
                onSuccess = { response ->
                    val aiAction = response.action ?: "zen"

                    // Check if an ambush is already waiting
                    val currentAmbush = prefs.getString("PENDING_AMBUSH_ACTION", null)

                    prefs.edit {
                        // 🟢 STEP 4: THE SAFEGUARD
                        if (aiAction != "zen" && aiAction != "Log_Only" && aiAction.isNotBlank()) {
                            // 1. It's a real game! Always overwrite and save it.
                            putString("LAST_AI_ACTION", aiAction)
                            putString("PENDING_AMBUSH_ACTION", aiAction)
                            putString("PENDING_AMBUSH_MESSAGE", response.gamificationMessage ?: "Time for a challenge!")
                            putString("PENDING_AMBUSH_ID", response.predictionId ?: "")

                        } else if (currentAmbush == null) {
                            // 2. It's "zen" AND there are no ambushes waiting. Safe to update.
                            putString("LAST_AI_ACTION", aiAction)
                        }
                        // 3. If it's "zen" but an ambush IS waiting, we do NOTHING to protect the ambush!
                    }
                },
                onFailure = {
                    // Silent background failure. The user's expense is safe.
                }
            )
        }
    }

    // 🟢 Safely delete from the database in the background
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense.id)
        }
    }

    // 🟢 Safely update the database in the background
    fun editExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.insertExpense(expense)
        }
    }
}

// 🟢 THE FACTORY
class ExpenseViewModelFactory(
    private val repository: FinanceRepository,
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, expenseDao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}