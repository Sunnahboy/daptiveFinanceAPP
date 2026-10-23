package com.finadapt.adaptivefinance

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.finadapt.adaptivefinance.core.navigation.NavGraph
import com.finadapt.adaptivefinance.data.local.AppDatabase
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.chat.ChatViewModel
import com.finadapt.adaptivefinance.feature.chat.ChatViewModelFactory
import com.finadapt.adaptivefinance.feature.community.CommunityViewModel
import com.finadapt.adaptivefinance.feature.community.CommunityViewModelFactory
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModel
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModelFactory
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModel
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModelFactory
import com.finadapt.adaptivefinance.worker.NotificationScheduler
import com.finadapt.adaptivefinance.ui.theme.AdaptiveFinanceTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import com.finadapt.adaptivefinance.ui.theme.AppTheme
import com.finadapt.adaptivefinance.ui.theme.LocalAppTheme
import com.finadapt.adaptivefinance.feature.splash.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android 12+ Splash Screen API using our rotating ring icon (`ic_animated_splash`)
        val splashScreen = installSplashScreen()

        // 2. Initialize the Room Database & Prefs early so we can build the ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)
        val repository = FinanceRepository(database.expenseDao(), prefs)

        // 3. Pre-build the DashboardViewModel so we can listen to its readiness state
        val dashboardFactory = DashboardViewModelFactory(database.expenseDao(), prefs)
        val dashboardViewModel: DashboardViewModel by viewModels { dashboardFactory }

        // 4. Keep the native splash screen on screen until data and artifacts are 100% ready
        splashScreen.setKeepOnScreenCondition { !dashboardViewModel.isDataLoaded.value }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load the user's saved times and schedule them all
        val savedTimes = NotificationScheduler.getTimesFromPrefs(this)
        NotificationScheduler.scheduleDailyReminders(this, savedTimes)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val isDataLoaded by dashboardViewModel.isDataLoaded.collectAsState()

            if (showSplash || !isDataLoaded) {
                // Seamless handoff: Compose SplashScreen renders with the exact same background & ring,
                // and adds "FinAdapt" and "Loading insights..." directly in the center.
                SplashScreen(
                    isReady = isDataLoaded,
                    onSplashFinished = { showSplash = false }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Prepare factories and build ViewModels
                    val expenseFactory = ExpenseViewModelFactory(repository, database.expenseDao(), prefs)
                    val expenseViewModel: ExpenseViewModel = viewModel(factory = expenseFactory)

                    // Build the Community ViewModel
                    val communityFactory = CommunityViewModelFactory(repository, dashboardViewModel)
                    val communityViewModel: CommunityViewModel = viewModel(factory = communityFactory)
                    val chatFactory = ChatViewModelFactory(database.expenseDao(), database.chatDao(), prefs)
                    val chatViewModel: ChatViewModel = viewModel(factory = chatFactory)

                    val currentThemeStr by dashboardViewModel.appTheme.collectAsState()
                    val appearanceMode by dashboardViewModel.appearanceMode.collectAsState()
                    val appTheme = AppTheme.fromString(currentThemeStr)

                    CompositionLocalProvider(LocalAppTheme provides appTheme) {
                        AdaptiveFinanceTheme(appTheme = appTheme, appearanceMode = appearanceMode) {
                            // Launch the App via NavGraph
                            NavGraph(
                                navController = navController,
                                dashboardViewModel = dashboardViewModel,
                                expenseViewModel = expenseViewModel,
                                communityViewModel = communityViewModel,
                                chatViewModel = chatViewModel,
                                prefs = prefs
                            )
                        }
                    }
                }
            }
        }
    }
}
