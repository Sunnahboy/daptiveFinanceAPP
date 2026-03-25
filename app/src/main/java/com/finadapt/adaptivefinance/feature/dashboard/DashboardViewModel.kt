
package com.finadapt.adaptivefinance.feature.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.core.content.edit
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    // ALL APP STATES
    private val _playCoinDropAnimation = MutableStateFlow(false)
    val playCoinDropAnimation: StateFlow<Boolean> = _playCoinDropAnimation.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _totalSpend = MutableStateFlow(0f)
    val totalSpend: StateFlow<Float> = _totalSpend.asStateFlow()

    private val _todaySpend = MutableStateFlow(0f)
    val todaySpend: StateFlow<Float> = _todaySpend.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0f)
    val monthlyBudget: StateFlow<Float> = _monthlyBudget.asStateFlow()

    private val _currentAiAction = MutableStateFlow("zen")
    val currentAiAction: StateFlow<String> = _currentAiAction.asStateFlow()

    // 🟢 1. MOUNTAIN PROGRESS (Lifetime XP - Never goes down)
    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    // 🟢 2. SPENDABLE WALLET (XP minus Purchases)
    private val _userCoins = MutableStateFlow(0)
    val userCoins: StateFlow<Int> = _userCoins.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _weeklyChartData = MutableStateFlow(List(7) { 0f })

    private val _shieldCount = MutableStateFlow(0)
    val shieldCount: StateFlow<Int> = _shieldCount.asStateFlow()

    private val _showLevelUpCelebration = MutableStateFlow<String?>(null)
    val showLevelUpCelebration: StateFlow<String?> = _showLevelUpCelebration.asStateFlow()

    val allExpenses: StateFlow<List<ExpenseEntity>> = expenseDao.getALLExpensesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentExpenses: StateFlow<List<ExpenseEntity>> = allExpenses
        .map { it.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadDashboardData() {
        viewModelScope.launch {
            _isDarkMode.value = financeRepository.isDarkMode()
            _monthlyBudget.value = prefs.getFloat("MONTHLY_BUDGET", 1000f)
            _currentAiAction.value = prefs.getString("LAST_AI_ACTION", "zen") ?: "zen"

            // 🟢 ECONOMY LOGIC: Calculate Lifetime Map Progress vs Spendable Wallet
            val currentXp = prefs.getInt("USER_XP", 0)
            val spentCoins = prefs.getInt("SPENT_COINS", 0)

            _userXp.value = currentXp // Drives the Mountain Map
            _userCoins.value = currentXp - spentCoins // Drives the Shield Store

            checkForLevelUp(currentXp)

            _userName.value = prefs.getString("USER_NAME", "User") ?: "User"
            _shieldCount.value = prefs.getInt("STREAK_SHIELDS", 0)
            _currentStreak.value = financeRepository.getLiveStreak()

            val shouldDropCoin = prefs.getBoolean("PENDING_COIN_DROP", false)
            if (shouldDropCoin){
                _playCoinDropAnimation.value = true
                prefs.edit { putBoolean("PENDING_COIN_DROP", false) }
            }

            val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
            val timeLimit = System.currentTimeMillis() - thirtyDaysInMillis
            _totalSpend.value = expenseDao.getTotalSpendTimeBounded(timeLimit) ?: 0f

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            _todaySpend.value = expenseDao.getTodaySpend(startOfDay) ?: 0f

            val today = System.currentTimeMillis()
            val sevenDaysAgo = today - (7L * 24 * 60 * 60 * 1000)
            val recentList = expenseDao.getExpensesSince(sevenDaysAgo)

            val dailyTotals = FloatArray(7)
            recentList.forEach { expense ->
                val daysAgo = ((today - expense.timestamp) / (24 * 60 * 60 * 1000)).toInt()
                if (daysAgo in 0..6) {
                    dailyTotals[6 - daysAgo] += expense.amount
                }
            }
            _weeklyChartData.value = dailyTotals.toList()

            val currentTier = when {
                currentXp < 500 -> "Bronze Novice"
                currentXp < 2000 -> "Silver Guardian"
                else -> "Gold Master"
            }

            try {
                financeRepository.syncLeaderboard(xp = currentXp, tier = currentTier)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // SETTINGS CONTROLS
    fun updateUserName(newName: String) {
        prefs.edit { putString("USER_NAME", newName) }
        _userName.value = newName
    }

    fun updateMonthlyBudget(newBudget: Float) {
        prefs.edit { putFloat("MONTHLY_BUDGET", newBudget) }
        _monthlyBudget.value = newBudget
    }

    fun resetGamification() {
        // 🟢 Reset both XP and Spent Coins so they don't get negative balances!
        prefs.edit {
            putInt("USER_XP", 0)
            putInt("SPENT_COINS", 0)
            putInt("CURRENT_STREAK", 0)
            putInt("STREAK_SHIELDS", 0)
            putString("LAST_AI_ACTION", "zen")

        }
        _userXp.value = 0
        _userCoins.value = 0
        _currentStreak.value = 0
        _shieldCount.value = 0
        _currentAiAction.value = "zen"
    }

    fun wipeAllData() {
        viewModelScope.launch {
            expenseDao.deleteAllExpenses()
            prefs.edit {
                putInt("CURRENT_STREAK", 0)
                putLong("LAST_LOGGED_MIDNIGHT", 0L)
                putInt("USER_XP", 0)
                putInt("SPENT_COINS", 0) // Reset spent coins too
                putInt("STREAK_SHIELDS", 0)
            }

            _currentStreak.value = 0
            _userXp.value = 0
            _userCoins.value = 0
            _shieldCount.value =0

            loadDashboardData()
        }
    }

    // 🟢 SAFE STORE PURCHASING LOGIC
    fun onBuyStreakShield() {
        viewModelScope.launch {
            val cost = 500
            val currentCoins = _userCoins.value

            if (currentCoins >= cost) {
                // 1. Calculate new totals
                val spentSoFar = prefs.getInt("SPENT_COINS", 0)
                val newSpentTotal = spentSoFar + cost
                val currentShields = prefs.getInt("STREAK_SHIELDS", 0)
                val newShieldTotal = currentShields + 1

                // 2. Save directly to SharedPreferences (Bypassing the old repo method)
                prefs.edit {
                    putInt("SPENT_COINS", newSpentTotal)
                    putInt("STREAK_SHIELDS", newShieldTotal)
                }

                // 3. Update the UI States instantly
                _userCoins.value = _userXp.value - newSpentTotal
                _shieldCount.value = newShieldTotal
            }
        }
    }

    private fun checkForLevelUp(currentXp: Int) {
        val currentTierName = when {
            currentXp < 500 -> "Bronze Novice"
            currentXp < 2000 -> "Silver Guardian"
            currentXp < 5000 -> "Gold Master"
            else -> "Platinum Legend"
        }

        val lastSeenTier = financeRepository.getLastSeenTier()

        if (currentTierName != lastSeenTier) {
            _showLevelUpCelebration.value = currentTierName
            financeRepository.saveLastSeenTier(currentTierName)
        }
    }

    fun dismissLevelUpCelebration() {
        _showLevelUpCelebration.value = null
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            financeRepository.saveThemePreference(isDark)
            _isDarkMode.value = isDark
        }
    }

    fun resetCoinDropAnimation(){
        _playCoinDropAnimation.value = false
    }
}