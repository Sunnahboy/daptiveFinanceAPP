
package com.finadapt.adaptivefinance.core.navigation

import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finadapt.adaptivefinance.feature.community.CommunityScreen
import com.finadapt.adaptivefinance.feature.community.CommunityViewModel
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
    communityViewModel: CommunityViewModel,
    prefs: SharedPreferences
) {
    // 1. Observe the current route to know which tab is active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 2. Only show the Bottom Bar on the main menu screens!
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Community.route,
        Screen.Rewards.route,
        Screen.AddExpense.route,

    )
    val isDark  by dashboardViewModel.isDarkMode.collectAsState()
    val navBarBgColor = if (isDark) Color(0xFF1E293B) else Color.White


    // 3. The Scaffold wrapper
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = navBarBgColor,
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
                    // COMMUNITY BUTTON
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Public, contentDescription = "Community") },
                        label = { Text("Community") },
                        selected = currentRoute == Screen.Community.route,
                        onClick = { navController.navigate(Screen.Community.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0284C7),
                            selectedTextColor = Color(0xFF0284C7),
                            indicatorColor = Color(0xFFE0F2FE)
                        )
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
                //data loading in LaunchedEffect to prevent infinite loops
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
                val isDark by dashboardViewModel.isDarkMode.collectAsState()
                val playCoinDrop by dashboardViewModel.playCoinDropAnimation.collectAsState()

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
                    isDarkMode = isDark,
                    playCoinDrop = playCoinDrop,
                    onAnimationFinished = { dashboardViewModel.resetCoinDropAnimation() },
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
                    // 🟢 THE FIX: Update the lambda to accept the 3 new parameters!
                    onFeedback = { predictionId, strategyName, userAccepted ->
                        expenseViewModel.submitFeedback(predictionId, strategyName, userAccepted)
                    },
                    onDismissState = {
                        expenseViewModel.resetState()
                        //fires ONCE when the button is clicked
                        dashboardViewModel.loadDashboardData()
                        navController.popBackStack()
                    }
                )
            }

            composable(route = Screen.History.route) {
                //Wrap data loading in LaunchedEffect
                LaunchedEffect(Unit) {
                    dashboardViewModel.loadDashboardData()
                }

                val allExpenses by dashboardViewModel.allExpenses.collectAsState()
                val isDark by dashboardViewModel.isDarkMode.collectAsState()
                HistoryScreen(
                    allExpenses = allExpenses,
                    isDarkMode = isDark
                )
            }

            composable(route = Screen.Community.route) {
                // Refresh leaderboard when arriving
                LaunchedEffect(Unit) {
                    communityViewModel.fetchLeaderboard()
                }

                val leaderboardData by communityViewModel.leaderboardData.collectAsState()
                val isLoading by communityViewModel.isLoading.collectAsState()
                val isDark by dashboardViewModel.isDarkMode.collectAsState()

                CommunityScreen(
                    leaderboardData = leaderboardData,
                    currentAnonName = communityViewModel.currentAnonName,
                    isLoading = isLoading,
                    isDarkMode = isDark
                )
            }

            composable(route = Screen.Rewards.route) {
                //Wrap data loading in LaunchedEffect
                LaunchedEffect(Unit) {
                    dashboardViewModel.loadDashboardData()
                }

                val userXp by dashboardViewModel.userXp.collectAsState()
                val shieldCount by dashboardViewModel.shieldCount.collectAsState()
                val liveBadges by dashboardViewModel.badges.collectAsState()
                val isDark by dashboardViewModel.isDarkMode.collectAsState()

                RewardsScreen(
                    userXp = userXp,
                    shieldCount = shieldCount,
                    badges = liveBadges,
                    isDarkMode = isDark,
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