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
import  com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.gamification.Badge


class DashboardViewModel(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    // ALL APP STATES
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

    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _recentExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val recentExpenses: StateFlow<List<ExpenseEntity>> = _recentExpenses.asStateFlow()

    private val _allExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val allExpenses: StateFlow<List<ExpenseEntity>> = _allExpenses.asStateFlow()

    private val _weeklyChartData = MutableStateFlow(List(7) { 0f })
    val weeklyChartData: StateFlow<List<Float>> = _weeklyChartData.asStateFlow()
    //Track the Shields in the UI State
    private val _shieldCount = MutableStateFlow(0)
    val shieldCount: StateFlow<Int> = _shieldCount.asStateFlow()

    // StateFlow to hold the dynamic badges
    private val _badges = MutableStateFlow<List<Badge>>(emptyList())
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    //The Celebration Trigger State
     private val _showLevelUpCelebration = MutableStateFlow<String?>(null)
    //HARDCODED FOR TESTING
    //private val _showLevelUpCelebration = MutableStateFlow<String?>("Silver Guardian 🛡️")
    val showLevelUpCelebration: StateFlow<String?> = _showLevelUpCelebration.asStateFlow()

    init {
        loadDashboardData()
    }

    // DATA FETCHING

    fun loadDashboardData() {
        viewModelScope.launch {
            // 1. Fetch Preferences
            _monthlyBudget.value = prefs.getFloat("MONTHLY_BUDGET", 1000f)
            _currentAiAction.value = prefs.getString("LAST_AI_ACTION", "zen") ?: "zen"
            _userXp.value = prefs.getInt("USER_XP", 0)
            checkForLevelUp(_userXp.value)
            _userName.value = prefs.getString("USER_NAME", "User") ?: "User"
            _shieldCount.value = financeRepository.getShieldCount()
            //Fetch the dynamic streak!
            _currentStreak.value = financeRepository.getLiveStreak()

            // 2. Fetch Total Monthly Spend (30-day window)
            val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
            val timeLimit = System.currentTimeMillis() - thirtyDaysInMillis
            _totalSpend.value = expenseDao.getTotalSpendTimeBounded(timeLimit) ?: 0f

            // 3. Fetch Today's Spend
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            _todaySpend.value = expenseDao.getTodaySpend(startOfDay) ?: 0f

            // 4. Fetch Ledger Data
            _recentExpenses.value = expenseDao.getRecentLedger()
            val allExp = expenseDao.getAllExpenses()
            _allExpenses.value = allExp
            evaluateBadges(allExp.size)


            // 5. Fetch 7-Day Chart Data for History Screen
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
        }
    }


    //The Badge Evaluator Engine
    private fun evaluateBadges(totalExpensesLogged: Int) {
        val streak = _currentStreak.value
        val quizWins = financeRepository.getGameStat("quiz")
        val strictBudgetWins = financeRepository.getGameStat("strict_budget")
        val coolOffWins = financeRepository.getGameStat("cool_off")

        val dynamicBadges = listOf(
            Badge(
                id = "1", title = "First Step", icon = "🎯",
                description = "Log your first expense\n($totalExpensesLogged/1)",
                isUnlocked = totalExpensesLogged >= 1
            ),
            Badge(
                id = "2", title = "Week Warrior", icon = "🔥",
                description = "Maintain a 7-day streak\n($streak/7)",
                isUnlocked = streak >= 7
            ),
            Badge(
                id = "3", title = "AI Scholar", icon = "🧠",
                description = "Pass 5 Financial Quizzes\n($quizWins/5)",
                isUnlocked = quizWins >= 5
            ),
            Badge(
                id = "4", title = "Iron Will", icon = "🛡️",
                description = "Lock budget 3 times\n($strictBudgetWins/3)",
                isUnlocked = strictBudgetWins >= 3
            ),
            Badge(
                id = "5", title = "Zen Master", icon = "❄️",
                description = "Use the Cool-Off timer\n($coolOffWins/1)",
                isUnlocked = coolOffWins >= 1
            ),
            Badge(
                id = "6", title = "Savings Starter", icon = "💰",
                description = "Log 10 total expenses\n($totalExpensesLogged/10)",
                isUnlocked = totalExpensesLogged >= 10
            )
        )

        _badges.value = dynamicBadges
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
        prefs.edit { putInt("USER_XP", 0).putString("LAST_AI_ACTION", "zen") }
        _userXp.value = 0
        _currentAiAction.value = "zen"
    }

    fun wipeAllData() {
        viewModelScope.launch {
            expenseDao.deleteAllExpenses()
            loadDashboardData() // Forces the UI to refresh back to RM 0.00
        }
    }

    // Save an expense and immediately refresh the UI
    fun saveExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.insertExpense(expense)
            loadDashboardData() // This forces the Donut Chart and XP to update instantly!
        }
    }


    //The Buy Action triggered by the UI button
    fun onBuyStreakShield() {
        viewModelScope.launch {
            val success = financeRepository.buyStreakShield()
            if (success) {
                //If it worked, immediately reload the data so the UI updates!
                _userXp.value = financeRepository.getUserXp() //Refresh XP
                _shieldCount.value = financeRepository.getShieldCount() // Refresh Shields
            }
        }
    }


    //The Detector Logic (called after fetching XP)
    private fun checkForLevelUp(currentXp: Int) {
        val currentTierName = when {
            currentXp < 500 -> "Bronze Novice"
            currentXp < 2000 -> "Silver Guardian"
            currentXp < 5000 -> "Gold Master"
            else -> "Platinum Legend"
        }

        // 🟢 FIX: Ask the repository for the data safely
        val lastSeenTier = financeRepository.getLastSeenTier()

        // If the names don't match, THEY LEVELED UP!
        if (currentTierName != lastSeenTier) {
            _showLevelUpCelebration.value = currentTierName // Trigger the UI

            // 🟢 FIX: Ask the repository to save the data safely
            financeRepository.saveLastSeenTier(currentTierName)
        }
    }

    //The Dismiss Function
    fun dismissLevelUpCelebration() {
        _showLevelUpCelebration.value = null
    }



}