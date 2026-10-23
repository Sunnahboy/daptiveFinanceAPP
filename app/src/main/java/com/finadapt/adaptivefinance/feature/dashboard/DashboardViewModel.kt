package com.finadapt.adaptivefinance.feature.dashboard

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import kotlinx.coroutines.delay
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

data class MascotUiState(
    val levelName: String = "Bronze Novice",
    val tierColorHex: Long = 0xFFD97706,
    val fillPercentage: Float = 0f,
    val xpText: String = "0 / 500 XP"
)

class DashboardViewModel(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    // ALL APP STATES
    private val _playCoinDropAnimation = MutableStateFlow(false)
    val playCoinDropAnimation: StateFlow<Boolean> = _playCoinDropAnimation.asStateFlow()

    private val _appearanceMode = MutableStateFlow("DARK")
    val appearanceMode: StateFlow<String> = _appearanceMode.asStateFlow()

    private val _appTheme = MutableStateFlow("EMERALD")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    // Pre-load completion flag for the native splash screen
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    private val _totalSpend = MutableStateFlow(0f)
    val totalSpend: StateFlow<Float> = _totalSpend.asStateFlow()

    private val _todaySpend = MutableStateFlow(0f)
    val todaySpend: StateFlow<Float> = _todaySpend.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0f)
    val monthlyBudget: StateFlow<Float> = _monthlyBudget.asStateFlow()

    private val _currencySymbol = MutableStateFlow("RM")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _currentAiAction = MutableStateFlow("zen")
    val currentAiAction: StateFlow<String> = _currentAiAction.asStateFlow()

    //MOUNTAIN PROGRESS (Lifetime XP - Never goes down)
    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    //SPENDABLE WALLET (Dedicated coin wallet)
    private val _userCoins = MutableStateFlow(0)
    val userCoins: StateFlow<Int> = _userCoins.asStateFlow()
    
    //The StateFlow to track how many times they beat the map
    private val _ascensionLevel = MutableStateFlow(0)
    val ascensionLevel: StateFlow<Int> = _ascensionLevel.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _weeklyChartData = MutableStateFlow(List(7) { 0f })

    private val _shieldCount = MutableStateFlow(0)
    val shieldCount: StateFlow<Int> = _shieldCount.asStateFlow()

    private val _showLevelUpCelebration = MutableStateFlow<String?>(null)
    val showLevelUpCelebration: StateFlow<String?> = _showLevelUpCelebration.asStateFlow()

    // Mascot state flow
    private val _mascotState = MutableStateFlow(MascotUiState())
    val mascotState: StateFlow<MascotUiState> = _mascotState.asStateFlow()

    private val _dynamicTitle = mutableStateOf("Hey Player 👋")
    val dynamicTitle: State<String> = _dynamicTitle

    private val _dynamicSubtitle = mutableStateOf("Let's stay on budget today.")
    val dynamicSubtitle: State<String> = _dynamicSubtitle

    // Helper to calculate the exact millisecond the current month started
    private fun getStartOfMonthMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    //reacts to EVERYTHING instantly
    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "USER_XP", "USER_COINS", "PRESTIGE_LEVEL" -> {
                val currentXp = prefs.getInt("USER_XP", 0)
                _userXp.value = currentXp
                _userCoins.value = prefs.getInt("USER_COINS", 0)
                _ascensionLevel.value = prefs.getInt("PRESTIGE_LEVEL", 0)
                
                //instantly updates the Mascot when XP changes
                calculateMascotState(currentXp) 
            }
            "CURRENT_STREAK" -> {
                // This instantly updates the UI when the streak changes!
                _currentStreak.value = financeRepository.getLiveStreak()
            }
            "PENDING_COIN_DROP" -> {
                //instantly triggers the Lottie animation
                val shouldDrop = prefs.getBoolean("PENDING_COIN_DROP", false)
                if (shouldDrop) {
                    _playCoinDropAnimation.value = true
                    prefs.edit { putBoolean("PENDING_COIN_DROP", false) } //Reset so it doesn't loop
                }
            }
            "USER_NAME" -> {
                _userName.value = prefs.getString("USER_NAME", "User") ?: "User"
                calculateBehavioralGreeting(allExpenses.value, _userName.value, _currencySymbol.value)
            }
            "PREFERRED_CURRENCY" -> {
                _currencySymbol.value = prefs.getString("PREFERRED_CURRENCY", "RM") ?: "RM"
                calculateBehavioralGreeting(allExpenses.value, _userName.value, _currencySymbol.value)
            }
            "APP_THEME" -> {
                _appTheme.value = prefs.getString("APP_THEME", "EMERALD") ?: "EMERALD"
            }
            "APPEARANCE_MODE" -> {
                _appearanceMode.value = prefs.getString("APPEARANCE_MODE", "DARK") ?: "DARK"
            }
        }
    }

    val allExpenses: StateFlow<List<ExpenseEntity>> = expenseDao.getALLExpensesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        viewModelScope.launch {
            // 1. Fully load dashboard data and compute all totals, charts, and preferences
            loadDashboardData()

            // 2. Ensure an unhurried, comfortable minimum loading duration (2.8 seconds) so the ring spins smoothly and everything is 100% prepped
            delay(2800L)

            // 3. Flag that everything is 100% ready before dismissing splash screen
            _isDataLoaded.value = true
        }

        // 4. Ongoing collection for live expense updates
        viewModelScope.launch {
            allExpenses.collect { expenses ->
                val startOfMonth = getStartOfMonthMillis()
                _totalSpend.value = expenseDao.getTotalSpendTimeBounded(
                    startOfMonth,
                    System.currentTimeMillis()
                ) ?: 0f

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
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
                calculateBehavioralGreeting(expenses, _userName.value, _currencySymbol.value)
            }
        }
    }

    val recentExpenses: StateFlow<List<ExpenseEntity>> = allExpenses
        .map { it.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadDashboardData() {
        viewModelScope.launch {
            _appearanceMode.value = financeRepository.getAppearanceMode()
            _appTheme.value = prefs.getString("APP_THEME", "EMERALD") ?: "EMERALD"
            _monthlyBudget.value = prefs.getFloat("MONTHLY_BUDGET", 1000f)
            _currencySymbol.value = prefs.getString("PREFERRED_CURRENCY", "RM") ?: "RM"
            _currentAiAction.value = prefs.getString("LAST_AI_ACTION", "zen") ?: "zen"

            val currentXp = prefs.getInt("USER_XP", 0)
            _ascensionLevel.value = prefs.getInt("PRESTIGE_LEVEL", 0)

            _userXp.value = currentXp 
            _userCoins.value = prefs.getInt("USER_COINS", 0) // Read direct wallet balance
            calculateMascotState(currentXp)
            checkForLevelUp(currentXp)

            _userName.value = prefs.getString("USER_NAME", "User") ?: "User"
            _shieldCount.value = prefs.getInt("STREAK_SHIELDS", 0)
            _currentStreak.value = financeRepository.getLiveStreak()

            //Calculate start of calendar month
            val startOfMonth = getStartOfMonthMillis()
            _totalSpend.value = expenseDao.getTotalSpendTimeBounded(
                startOfMonth,
                System.currentTimeMillis()
            ) ?: 0f

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

            // Update greeting after fetching data
            calculateBehavioralGreeting(allExpenses.value, _userName.value, _currencySymbol.value)

            val currentTier = when {
                currentXp < 500 -> "Bronze Novice"
                currentXp < 2000 -> "Silver Guardian"
                currentXp < 5000 -> "Gold Master"
                else -> "Platinum Legend"
            }

            try {
                financeRepository.syncLeaderboard(xp = currentXp, tier = currentTier)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    @SuppressLint("DefaultLocale")
    fun calculateBehavioralGreeting(expenses: List<ExpenseEntity>, userName: String, currencySymbol: String) {
        val cleanName = userName.ifEmpty { "Player" }
        
        // Filter to ONLY current month so AI comments reset with the month
        val startOfMonth = getStartOfMonthMillis()
        val currentMonthExpenses = expenses.filter { it.timestamp >= startOfMonth }

        if (currentMonthExpenses.isEmpty()) {
            _dynamicTitle.value = "Hey $cleanName 👋"
            _dynamicSubtitle.value = "A fresh canvas! Log an expense to start your journey. 🌱"
            return
        }

        // 1. Find the top spending category and its total sum for THIS month
        val topCategory = currentMonthExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
            .maxByOrNull { it.value }

        if (topCategory != null && topCategory.value > 0f) {
            val category = topCategory.key
            val totalSpent = topCategory.value
            val formattedAmount = String.format("%.0f", totalSpent)

            //Check if it's just a small amount first
            if (totalSpent < 100f) {
                _dynamicTitle.value = "Off to a great start, $cleanName! 🚀"
                _dynamicSubtitle.value = "You've spent $currencySymbol $formattedAmount on $category so far. Keeping it light!"
                return
            }

            //If it's over currency 100, THEN hit them with the behavioral warnings
            when (category.lowercase()) {
                "food", "dining", "groceries" -> {
                    _dynamicTitle.value = "Hey Foodie Boss $cleanName! 🍔"
                    _dynamicSubtitle.value = "Your appetite cost $currencySymbol $formattedAmount this month. Time to scale back?"
                }
                "transport", "gas", "parking" -> {
                    _dynamicTitle.value = "Hey Jetsetter $cleanName! 🚗"
                    _dynamicSubtitle.value = "You've driven through $currencySymbol $formattedAmount on Transport recently."
                }
                "shopping", "clothes" -> {
                    _dynamicTitle.value = "Hey Big Spender $cleanName! 🛍️"
                    _dynamicSubtitle.value = "Shopping is your #1 drain right now at $currencySymbol $formattedAmount."
                }
                "entertainment", "games", "movies" -> {
                    _dynamicTitle.value = "Hey Player One $cleanName! 🎮"
                    _dynamicSubtitle.value = "$currencySymbol $formattedAmount spent on fun. Let's lock it down."
                }
                else -> {
                    _dynamicTitle.value = "Hey Chief $cleanName! 📈"
                    _dynamicSubtitle.value = "$category is taking over your wallet at $currencySymbol $formattedAmount."
                }
            }
        } else {
            _dynamicTitle.value = "Hey $cleanName 👋"
            _dynamicSubtitle.value = "Let's stay on budget today."
        }
    }

    fun refreshCoins() {
        _userCoins.value = prefs.getInt("USER_COINS", 0)
    }

    // SETTINGS CONTROLS
    fun updateUserName(newName: String) {
        prefs.edit { putString("USER_NAME", newName) }
        _userName.value = newName
        calculateBehavioralGreeting(allExpenses.value, newName, _currencySymbol.value)
    }

    fun updateCurrency(newCurrency: String) {
        prefs.edit { putString("PREFERRED_CURRENCY", newCurrency) }
        _currencySymbol.value = newCurrency
    }

    fun updateMonthlyBudget(newBudget: Float) {
        prefs.edit { putFloat("MONTHLY_BUDGET", newBudget) }
        _monthlyBudget.value = newBudget
    }

    fun resetGamification() {
        prefs.edit {
            putInt("USER_XP", 0)
            putInt("USER_COINS", 0)
            putInt("CURRENT_STREAK", 0)
            putInt("STREAK_SHIELDS", 0)
            putString("LAST_AI_ACTION", "zen")
            putLong("LAST_LOGGED_MIDNIGHT", 0L)
        }
        
        _userXp.value = 0
        _userCoins.value = 0
        _currentStreak.value = 0
        _shieldCount.value = 0
        _currentAiAction.value = "zen"
        
        //Force the Mascot back to the sad/baseline state instantly
        calculateMascotState(0) 
    }

    fun wipeAllData() {
        viewModelScope.launch {
            expenseDao.deleteAllExpenses()
            prefs.edit {
                putInt("CURRENT_STREAK", 0)
                putLong("LAST_LOGGED_MIDNIGHT", 0L)
                putInt("USER_XP", 0)
                putInt("USER_COINS", 0) // Reset standalone wallet
                putInt("STREAK_SHIELDS", 0)
            }

            _currentStreak.value = 0
            _userXp.value = 0
            _userCoins.value = 0
            _shieldCount.value = 0

            loadDashboardData()
        }
    }

    // SAFE STORE PURCHASING LOGIC
    fun onBuyStreakShield() {
        viewModelScope.launch {
            val cost = 500
            val currentCoins = prefs.getInt("USER_COINS", 0)

            if (currentCoins >= cost) {
                val currentShields = prefs.getInt("STREAK_SHIELDS", 0)
                val newShieldTotal = currentShields + 1

                prefs.edit {
                    putInt("USER_COINS", currentCoins - cost) // Direct deduction
                    putInt("STREAK_SHIELDS", newShieldTotal)
                }
                
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

    fun setAppearanceMode(mode: String) {
        viewModelScope.launch {
            financeRepository.saveAppearanceMode(mode)
            _appearanceMode.value = mode
        }
    }

    fun updateAppTheme(theme: String) {
        prefs.edit { putString("APP_THEME", theme) }
        _appTheme.value = theme
    }

    fun resetCoinDropAnimation(){
        _playCoinDropAnimation.value = false
    }

    fun submitFeedback(predictionId: String, strategyName: String, userAccepted: Boolean) {
        viewModelScope.launch {
            financeRepository.submitUserFeedback(predictionId, strategyName, userAccepted)
        }
    }

    private fun calculateMascotState(currentXp: Int) {
        val levelName: String
        val tierColorHex: Long
        val currentLevelMin: Int
        val nextLevelMax: Int

        when {
            currentXp < 500 -> {
                levelName = "Bronze Novice"
                tierColorHex = 0xFFD97706
                currentLevelMin = 0
                nextLevelMax = 500
            }
            currentXp < 2000 -> {
                levelName = "Silver Guardian"
                tierColorHex = 0xFF94A3B8
                currentLevelMin = 500
                nextLevelMax = 2000
            }
            currentXp < 5000 -> {
                levelName = "Gold Master"
                tierColorHex = 0xFFF59E0B
                currentLevelMin = 2000
                nextLevelMax = 5000
            }
            else -> {
                levelName = "Platinum Legend"
                tierColorHex = 0xFF34D399

                // THE PRESTIGE LOOP
                val excess = currentXp - 5000
                currentLevelMin = 5000 + (excess / 1000) * 1000
                nextLevelMax = currentLevelMin + 1000
            }
        }

        val xpIntoCurrentLevel = currentXp - currentLevelMin
        val xpRequiredForNextLevel = nextLevelMax - currentLevelMin

        val rawFillPercentage = if (xpRequiredForNextLevel > 0) {
            xpIntoCurrentLevel.toFloat() / xpRequiredForNextLevel.toFloat()
        } else {
            1f
        }

        val xpText = "$xpIntoCurrentLevel / $xpRequiredForNextLevel XP"

        // Emit the final calculated state to the UI
        _mascotState.value = MascotUiState(
            levelName = levelName,
            tierColorHex = tierColorHex,
            fillPercentage = rawFillPercentage.coerceIn(0f, 1f),
            xpText = xpText
        )
    }

    //THE PREMIER LEAGUE ASCENSION
    fun onAscend() {
        _userXp.value = 0
        _ascensionLevel.value += 1

        prefs.edit {
            putInt("PRESTIGE_LEVEL", _ascensionLevel.value)
            putInt("USER_XP", 0) // Reset map points only
        }

        calculateMascotState(0)
        
        viewModelScope.launch {
            try {
                financeRepository.syncLeaderboard(xp = 0, tier = "Bronze Novice")
                financeRepository.saveLastSeenTier("Bronze Novice")
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }
}
