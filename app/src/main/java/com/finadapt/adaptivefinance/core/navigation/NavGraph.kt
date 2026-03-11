//package com.finadapt.adaptivefinance.core.navigation
//
//import android.content.SharedPreferences
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.List
//import androidx.compose.material.icons.filled.EmojiEvents
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.filled.Settings
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import com.finadapt.adaptivefinance.feature.dashboard.DashboardScreen
//import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModel
//import com.finadapt.adaptivefinance.feature.expense.AddExpenseScreen
//import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModel
//import com.finadapt.adaptivefinance.feature.expense.history.HistoryScreen
//import com.finadapt.adaptivefinance.feature.expense.settings.SettingsScreen
//import com.finadapt.adaptivefinance.feature.gamification.RewardsScreen
//import com.finadapt.adaptivefinance.feature.onboarding.OnboardingScreen
//
//
//
//@Composable
//fun NavGraph(
//    navController: NavHostController,
//    startDestination: String,
//    dashboardViewModel: DashboardViewModel,
//    expenseViewModel: ExpenseViewModel,
//    prefs: SharedPreferences
//) {
//    // 1. Observe the current route to know which tab is active
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStackEntry?.destination?.route
//
//    // 2. Only show the Bottom Bar on the main menu screens!
//    val showBottomBar = currentRoute in listOf(
//        Screen.Dashboard.route,
//        Screen.History.route,
//        Screen.Rewards.route,
//        Screen.AddExpense.route,
//        Screen.Settings.route
//    )
//
//    // 3. The Scaffold wrapper
//    Scaffold(
//        bottomBar = {
//            if (showBottomBar) {
//                NavigationBar(
//                    containerColor = Color.White,
//                    tonalElevation = 8.dp
//                ) {
//                    // HOME BUTTON
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
//                        label = { Text("Home") },
//                        selected = currentRoute == Screen.Dashboard.route,
//                        onClick = {
//                            navController.navigate(Screen.Dashboard.route) {
//                                popUpTo(Screen.Dashboard.route) { inclusive = true }
//                            }
//                        },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = Color(0xFF10B981),
//                            selectedTextColor = Color(0xFF10B981),
//                            indicatorColor = Color(0xFFD1FAE5)
//                        )
//                    )
//
//                    // HISTORY BUTTON
//                    NavigationBarItem(
//                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
//                        label = { Text("History") },
//                        selected = currentRoute == Screen.History.route,
//                        onClick = { navController.navigate(Screen.History.route) }
//                    )
//
//                    // 🟢 2. REWARDS BUTTON (The Trophy!)
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Rewards") },
//                        label = { Text("Rewards") },
//                        selected = currentRoute == Screen.Rewards.route,
//                        onClick = { navController.navigate(Screen.Rewards.route) },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = Color(0xFFF59E0B), // Gold color for the trophy
//                            selectedTextColor = Color(0xFFF59E0B),
//                            indicatorColor = Color(0xFFFEF3C7)     // Light gold background when selected
//                        )
//                    )
//
//                    // SETTINGS BUTTON
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
//                        label = { Text("Settings") },
//                        selected = currentRoute == Screen.Settings.route,
//                        onClick = { navController.navigate(Screen.Settings.route) }
//                    )
//                }
//            }
//        }
//    ) { innerPadding ->
//        // 4. The NavHost now lives INSIDE the Scaffold, respecting its padding
//        NavHost(
//            navController = navController,
//            startDestination = startDestination,
//            modifier = Modifier.padding(innerPadding)
//        ) {
//
//            composable(route = Screen.Onboarding.route) {
//                OnboardingScreen(
//                    onFinish = {
//                        navController.navigate(Screen.Dashboard.route) {
//                            popUpTo(Screen.Onboarding.route) { inclusive = true }
//                        }
//                    }
//                )
//            }
//
//            composable(route = Screen.Dashboard.route) {
//                dashboardViewModel.loadDashboardData()
//                val totalSpend by dashboardViewModel.totalSpend.collectAsState()
//                val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
//                val currentAiAction by dashboardViewModel.currentAiAction.collectAsState()
//                //collect the xp state
//                val userXp by dashboardViewModel.userXp.collectAsState()
//                //Collect the userName state
//                val userName by dashboardViewModel.userName.collectAsState()
//                // Collect Today's Spend
//                val todaySpend by dashboardViewModel.todaySpend.collectAsState()
//
//                //Collect the Chart and Ledger Data
//                val recentExpenses by dashboardViewModel.recentExpenses.collectAsState()
//                //collect the streak state from the viewModel
//                val currentStreak by dashboardViewModel.currentStreak.collectAsState()
//                val levelUpTier by dashboardViewModel.showLevelUpCelebration.collectAsState()
//
//
//                DashboardScreen(
//                    userName = userName, //Pass it to the screen!
//                    totalSpend = totalSpend,
//                    todaySpend = todaySpend,
//                    monthlyBudget = monthlyBudget,
//                    currentAiAction = currentAiAction,
//                    userXp = userXp,
//                    currentStreak = currentStreak,
//                    recentExpenses = recentExpenses,
//                    levelUpTier = levelUpTier,
//                    onDismissLevelUp = { dashboardViewModel.dismissLevelUpCelebration() },
//                    onNavigateToLogExpense = { navController.navigate(Screen.AddExpense.route) },
//                    onNavigateToSettings = { navController.navigate(Screen.Settings.route)}
//                )
//            }
//
//            composable(route = Screen.AddExpense.route) {
//                val uiState by expenseViewModel.uiState.collectAsState()
//                val realUserId = prefs.getString("SILENT_USER_ID", "fallback_id") ?: "fallback_id"
//
//                AddExpenseScreen(
//                    uiState = uiState,
//                    onLogExpense = { amount, category ->
//                        expenseViewModel.submitExpense(amount, category, realUserId)
//                    },
//                    onFeedback = { predictionId, reward ->
//                        expenseViewModel.submitFeedback(predictionId, reward)
//                    },
//                    onDismissState = {
//                        expenseViewModel.resetState()
//                        dashboardViewModel.loadDashboardData()
//                        navController.popBackStack()
//                    }
//                )
//            }
//
//            composable(route = Screen.History.route) {
//                // Ensure the ViewModel has the freshest data
//                dashboardViewModel.loadDashboardData()
//
//                // Collect the states we need for this specific screen
//                //val weeklyChartData by dashboardViewModel.weeklyChartData.collectAsState()
//                val allExpenses by dashboardViewModel.allExpenses.collectAsState()
//
//                HistoryScreen(
//                    //weeklyChartData = weeklyChartData,
//                    allExpenses = allExpenses
//                )
//            }
//
//            // 🟢 3. THE NEW REWARDS ROUTE
//            composable(route = Screen.Rewards.route) {
//                // Force a data refresh so XP is always perfectly accurate when they open the store
//                dashboardViewModel.loadDashboardData()
//
//                // Collect the state needed for the economy
//                val userXp by dashboardViewModel.userXp.collectAsState()
//                val shieldCount by dashboardViewModel.shieldCount.collectAsState()
//                val liveBadges by dashboardViewModel.badges.collectAsState()
//
//                RewardsScreen(
//                    userXp = userXp,
//                    shieldCount = shieldCount,
//                    badges = liveBadges,
//                    onBuyShield = { dashboardViewModel.onBuyStreakShield() } // Triggers the math!
//                )
//            }
//
//            composable(route = Screen.Settings.route) {
//                // Collect the current data
//                val userName by dashboardViewModel.userName.collectAsState()
//                val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
//                val isDark by dashboardViewModel.isDarkMode.collectAsState()
//                SettingsScreen(
//                    currentName = userName,
//                    currentBudget = monthlyBudget,
//                    isDarkMode = isDark,
//                    onNameChanged = { dashboardViewModel.updateUserName(it) },
//                    onBudgetChanged = { dashboardViewModel.updateMonthlyBudget(it) },
//                    onThemeToggled = { newTheme ->
//                        dashboardViewModel.toggleTheme(newTheme)
//                    },
//                    onResetGamification = { dashboardViewModel.resetGamification() },
//                    onWipeData = { dashboardViewModel.wipeAllData() },
//                    onNavigateBack = { navController.popBackStack() }
//                )
//            }
//        }
//    }
//}


package com.finadapt.adaptivefinance.core.navigation

import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finadapt.adaptivefinance.feature.dashboard.DashboardScreen
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModel
import com.finadapt.adaptivefinance.feature.expense.AddExpenseScreen
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModel
import com.finadapt.adaptivefinance.feature.expense.history.HistoryScreen
import com.finadapt.adaptivefinance.feature.expense.settings.SettingsScreen
import com.finadapt.adaptivefinance.feature.gamification.RewardsScreen
import com.finadapt.adaptivefinance.feature.onboarding.OnboardingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    dashboardViewModel: DashboardViewModel,
    expenseViewModel: ExpenseViewModel,
    prefs: SharedPreferences
) {
    // 1. Observe the current route to know which tab is active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 2. Only show the Bottom Bar on the main menu screens!
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Rewards.route,
        Screen.AddExpense.route,
        Screen.Settings.route
    )

    // 3. The Scaffold wrapper
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    // HOME BUTTON
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF10B981),
                            selectedTextColor = Color(0xFF10B981),
                            indicatorColor = Color(0xFFD1FAE5)
                        )
                    )

                    // HISTORY BUTTON
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                        label = { Text("History") },
                        selected = currentRoute == Screen.History.route,
                        onClick = { navController.navigate(Screen.History.route) }
                    )

                    // REWARDS BUTTON (The Trophy!)
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Rewards") },
                        label = { Text("Rewards") },
                        selected = currentRoute == Screen.Rewards.route,
                        onClick = { navController.navigate(Screen.Rewards.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFF59E0B),
                            selectedTextColor = Color(0xFFF59E0B),
                            indicatorColor = Color(0xFFFEF3C7)
                        )
                    )

                    // SETTINGS BUTTON
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 4. The NavHost now lives INSIDE the Scaffold, respecting its padding
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(route = Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Dashboard.route) {
                // 🟢 FIX: Wrap data loading in LaunchedEffect to prevent infinite loops
                LaunchedEffect(Unit) {
                    dashboardViewModel.loadDashboardData()
                }

                val totalSpend by dashboardViewModel.totalSpend.collectAsState()
                val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
                val currentAiAction by dashboardViewModel.currentAiAction.collectAsState()
                val userXp by dashboardViewModel.userXp.collectAsState()
                val userName by dashboardViewModel.userName.collectAsState()
                val todaySpend by dashboardViewModel.todaySpend.collectAsState()
                val recentExpenses by dashboardViewModel.recentExpenses.collectAsState()
                val currentStreak by dashboardViewModel.currentStreak.collectAsState()
                val levelUpTier by dashboardViewModel.showLevelUpCelebration.collectAsState()

                DashboardScreen(
                    userName = userName,
                    totalSpend = totalSpend,
                    todaySpend = todaySpend,
                    monthlyBudget = monthlyBudget,
                    currentAiAction = currentAiAction,
                    userXp = userXp,
                    currentStreak = currentStreak,
                    recentExpenses = recentExpenses,
                    levelUpTier = levelUpTier,
                    onDismissLevelUp = { dashboardViewModel.dismissLevelUpCelebration() },
                    onNavigateToLogExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route)}
                )
            }

            composable(route = Screen.AddExpense.route) {
                val uiState by expenseViewModel.uiState.collectAsState()
                val realUserId = prefs.getString("SILENT_USER_ID", "fallback_id") ?: "fallback_id"

                AddExpenseScreen(
                    uiState = uiState,
                    onLogExpense = { amount, category ->
                        expenseViewModel.submitExpense(amount, category, realUserId)
                    },
                    onFeedback = { predictionId, reward ->
                        expenseViewModel.submitFeedback(predictionId, reward)
                    },
                    onDismissState = {
                        expenseViewModel.resetState()
                        // 🟢 FIX: Safe to call here because it only fires ONCE when the button is clicked
                        dashboardViewModel.loadDashboardData()
                        navController.popBackStack()
                    }
                )
            }

            composable(route = Screen.History.route) {
                // 🟢 FIX: Wrap data loading in LaunchedEffect
                LaunchedEffect(Unit) {
                    dashboardViewModel.loadDashboardData()
                }

                val allExpenses by dashboardViewModel.allExpenses.collectAsState()

                HistoryScreen(
                    allExpenses = allExpenses
                )
            }

            composable(route = Screen.Rewards.route) {
                // 🟢 FIX: Wrap data loading in LaunchedEffect
                LaunchedEffect(Unit) {
                    dashboardViewModel.loadDashboardData()
                }

                val userXp by dashboardViewModel.userXp.collectAsState()
                val shieldCount by dashboardViewModel.shieldCount.collectAsState()
                val liveBadges by dashboardViewModel.badges.collectAsState()

                RewardsScreen(
                    userXp = userXp,
                    shieldCount = shieldCount,
                    badges = liveBadges,
                    onBuyShield = { dashboardViewModel.onBuyStreakShield() }
                )
            }

            composable(route = Screen.Settings.route) {
                // Collect the current data
                val userName by dashboardViewModel.userName.collectAsState()
                val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
                val isDark by dashboardViewModel.isDarkMode.collectAsState()

                SettingsScreen(
                    currentName = userName,
                    currentBudget = monthlyBudget,
                    isDarkMode = isDark,
                    onNameChanged = { dashboardViewModel.updateUserName(it) },
                    onBudgetChanged = { dashboardViewModel.updateMonthlyBudget(it) },
                    onThemeToggled = { newTheme ->
                        dashboardViewModel.toggleTheme(newTheme)
                    },
                    onResetGamification = { dashboardViewModel.resetGamification() },
                    onWipeData = { dashboardViewModel.wipeAllData() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}