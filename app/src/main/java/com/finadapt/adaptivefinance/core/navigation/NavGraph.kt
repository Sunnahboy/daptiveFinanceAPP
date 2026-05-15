
package com.finadapt.adaptivefinance.core.navigation
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finadapt.adaptivefinance.feature.chat.ChatScreen
import com.finadapt.adaptivefinance.feature.chat.ChatViewModel
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.input.pointer.pointerInput
import com.finadapt.adaptivefinance.ui.components.AnimatedDraggableFab
import kotlin.math.abs
@Composable
fun NavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    expenseViewModel: ExpenseViewModel,
    communityViewModel: CommunityViewModel,
    chatViewModel: ChatViewModel,
    prefs: SharedPreferences
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val hasCompletedOnboarding = prefs.getString("SILENT_USER_ID", null) != null
    val calculatedStartDestination = if (hasCompletedOnboarding) "MAIN_PAGER" else Screen.Onboarding.route

    val tabScreens = listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.AddExpense.route,
        Screen.Community.route,
        Screen.Rewards.route,
        Screen.Chat.route,
        Screen.Settings.route
    )

    val pagerState = rememberPagerState(pageCount = { tabScreens.size })
    val isDark by dashboardViewModel.isDarkMode.collectAsState()
    val navBarBgColor = if (isDark) Color(0xFF1E293B) else Color.White

    Scaffold(
        floatingActionButton = {
            val showFabs = currentRoute == "MAIN_PAGER" && pagerState.currentPage in listOf(0, 1, 3, 4)

            if (showFabs) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    //AI Chatbot Button
                    AnimatedDraggableFab(
                        text = "Ask AI...",
                        icon = Icons.Default.SmartToy,
                        contentDescription = "AI Assistant",
                        bubbleColor = Color(0xFF0284C7),
                        buttonContainerColor = Color.White,
                        buttonContentColor = Color(0xFF0284C7),
                        delayMillis = 1500L,
                        onClick = { scope.launch { pagerState.animateScrollToPage(5) } }
                    )

                    //Log Expense Button
                    AnimatedDraggableFab(
                        text = "Log...",
                        icon = Icons.Default.Add,
                        contentDescription = "Log Expense",
                        bubbleColor = Color(0xFF10B981),
                        buttonContainerColor = Color(0xFF10B981),
                        buttonContentColor = Color.White,
                        delayMillis = 2000L,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                }
            }
        },
        bottomBar = {
            val showNavBar = currentRoute == "MAIN_PAGER" && pagerState.currentPage < 5
            if (showNavBar) {
                NavigationBar(
                    containerColor = navBarBgColor,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        "Home" to Icons.Default.Home,
                        "History" to Icons.AutoMirrored.Filled.List,
                        "Log" to Icons.Default.AddCircle,
                        "Community" to Icons.Default.Public,
                        "Rewards" to Icons.Default.EmojiEvents
                    )

                    tabs.forEachIndexed { index, (label, icon) ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            label = { Text(label) },
                            icon = { Icon(icon, contentDescription = label) },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            colors = when (index) {
                                0 -> NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF10B981), indicatorColor = Color(0xFFD1FAE5))
                                2 -> NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF8B5CF6), indicatorColor = Color(0xFFEDE9FE))
                                3 -> NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), indicatorColor = Color(0xFFE0F2FE))
                                4 -> NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFF59E0B), indicatorColor = Color(0xFFFEF3C7))
                                else -> NavigationBarItemDefaults.colors()
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = calculatedStartDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Onboarding.route) {
                OnboardingScreen(onFinish = {
                    navController.navigate("MAIN_PAGER") { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                })
            }

            composable(route = "MAIN_PAGER") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 3,
                    //Disable normal pager swiping only when on Settings
                    userScrollEnabled = pagerState.currentPage != 6
                ) { pageIndex ->
                    when (tabScreens[pageIndex]) {

//
                        Screen.Dashboard.route -> {
                            LaunchedEffect(Unit) { dashboardViewModel.loadDashboardData() }
                            val totalSpend by dashboardViewModel.totalSpend.collectAsState()
                            val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
                            val todaySpend by dashboardViewModel.todaySpend.collectAsState()
                            val recentExpenses by dashboardViewModel.recentExpenses.collectAsState()
                            val userName by dashboardViewModel.userName.collectAsState()
                            val currentStreak by dashboardViewModel.currentStreak.collectAsState()
                            val isDarkDashboard by dashboardViewModel.isDarkMode.collectAsState()
                            val mascotState by dashboardViewModel.mascotState.collectAsState()

                            DashboardScreen(
                                userName = userName,
                                totalSpend = totalSpend,
                                todaySpend = todaySpend,
                                monthlyBudget = monthlyBudget,
                                currentStreak = currentStreak,
                                recentExpenses = recentExpenses,
                                isDarkMode = isDarkDashboard,
                                onNavigateToSettings = { scope.launch { pagerState.animateScrollToPage(6) } },
                                currentAiAction = dashboardViewModel.currentAiAction.collectAsState().value,
                                levelUpTier = dashboardViewModel.showLevelUpCelebration.collectAsState().value,
                                playCoinDrop = dashboardViewModel.playCoinDropAnimation.collectAsState().value,
                                onAnimationFinished = { dashboardViewModel.resetCoinDropAnimation() },
                                onDismissLevelUp = { dashboardViewModel.dismissLevelUpCelebration() },
                                onGameFeedback = { id, s, a -> dashboardViewModel.submitFeedback(id, s, a)},

                                // These handle all the Mascot math now:
                                levelName = mascotState.levelName,
                                tierColorHex = mascotState.tierColorHex,
                                fillPercentage = mascotState.fillPercentage,
                                xpText = mascotState.xpText
                            )
                        }

                        Screen.History.route -> {
                            val allExpenses by dashboardViewModel.allExpenses.collectAsState()
                            val isDarkHistory by dashboardViewModel.isDarkMode.collectAsState()
                            HistoryScreen(
                                allExpenses = allExpenses, isDarkMode = isDarkHistory,
                                onDeleteExpense = { expenseViewModel.deleteExpense(it) },
                                onEditExpense = { expenseViewModel.editExpense(it) }
                            )
                        }

                        Screen.AddExpense.route -> {
                            val isDarkAddExpense by dashboardViewModel.isDarkMode.collectAsState()
                            AddExpenseScreen(
                                isDarkMode = isDarkAddExpense,
                                onLogExpense = { a, c, m, d, p, i, itms -> expenseViewModel.logExpense(a, c, m, d, p, i, itms) },
                                onDismissState = {
                                    dashboardViewModel.loadDashboardData()
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                        }

                        Screen.Community.route -> {

                            //The ViewModel's init block handles the real-time stream automatically

                            val leaderboardData by communityViewModel.leaderboardData.collectAsState()
                            val isLoading by communityViewModel.isLoading.collectAsState()
                            val isDarkComm by dashboardViewModel.isDarkMode.collectAsState()
                            val hallOfFameData by communityViewModel.hallOfFameData.collectAsState()
                            val spendableCoins by communityViewModel.spendableCoins.collectAsState()


                            CommunityScreen(
                                leaderboardData = leaderboardData,
                                currentAnonName = communityViewModel.currentAnonName,
                                isLoading = isLoading,
                                isDarkMode = isDarkComm,
                                hallOfFameData = hallOfFameData,
                                spendableCoins = spendableCoins,
                                        // onCheerClicked action to hook the UI button to the ViewModel!
                                onCheerClicked = { targetUserId, targetAnonName ->
                                    communityViewModel.sendCheerToUser(targetUserId, targetAnonName)
                                }
                            )
                        }

                        Screen.Rewards.route -> {
                            val userXp by dashboardViewModel.userXp.collectAsState()
                            val shieldCount by dashboardViewModel.shieldCount.collectAsState()
                            val userCoins by dashboardViewModel.userCoins.collectAsState()
                            val isDarkRewards by dashboardViewModel.isDarkMode.collectAsState()
                            RewardsScreen(
                                userXp = userXp, shieldCount = shieldCount, userCoins = userCoins,
                                isDarkMode = isDarkRewards, onBuyShield = { dashboardViewModel.onBuyStreakShield() }
                            )
                        }

                        Screen.Chat.route -> {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateBack = { scope.launch { pagerState.animateScrollToPage(0) } } // Back to Home
                            )
                        }

                        Screen.Settings.route -> {
                            val userName by dashboardViewModel.userName.collectAsState()
                            val monthlyBudget by dashboardViewModel.monthlyBudget.collectAsState()
                            val isDarkSetting by dashboardViewModel.isDarkMode.collectAsState()
                            val liveExpenses by dashboardViewModel.allExpenses.collectAsState()

                            //custom swipe to the home screen
                            var dragAccumulator by remember { mutableFloatStateOf(0f) }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { dragAccumulator = 0f },
                                            onDragEnd = {
                                                //If  swiped left or right far enough, go Home 0
                                                if (abs(dragAccumulator) > 50f) {
                                                    scope.launch { pagerState.animateScrollToPage(0) }
                                                }
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumulator += dragAmount
                                            }
                                        )
                                    }
                            ) {
                                SettingsScreen(
                                    currentName = userName, currentBudget = monthlyBudget, isDarkMode = isDarkSetting,
                                    allExpenses = liveExpenses, onNameChanged = { dashboardViewModel.updateUserName(it) },
                                    onBudgetChanged = { dashboardViewModel.updateMonthlyBudget(it) },
                                    onThemeToggled = { dashboardViewModel.toggleTheme(it) },
                                    onResetGamification = { dashboardViewModel.resetGamification() },
                                    onWipeData = { dashboardViewModel.wipeAllData() },
                                    onNavigateBack = { scope.launch { pagerState.animateScrollToPage(0) } } // Back to Home
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}