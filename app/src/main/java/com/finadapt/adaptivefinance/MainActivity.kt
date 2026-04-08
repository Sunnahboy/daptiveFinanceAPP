package com.finadapt.adaptivefinance

import android.os.Bundle
import androidx.activity.ComponentActivity
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

// 🟢 NEW: Make sure to import your custom theme!
import com.finadapt.adaptivefinance.ui.theme.AdaptiveFinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🟢 Load the user's saved times and schedule them all!
        val savedTimes = NotificationScheduler.getTimesFromPrefs(this)
        NotificationScheduler.scheduleDailyReminders(this, savedTimes)

        // 1. Initialize the Room Database & Prefs
        val database = AppDatabase.getDatabase(applicationContext)
        val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)

        // 2. Give BOTH the Database and Prefs to the repository
        val repository = FinanceRepository(database.expenseDao(), prefs)

        setContent {
            // 🟢 THE FIX: Replaced 'MaterialTheme' with your custom 'AdaptiveFinanceTheme'
            AdaptiveFinanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 3. Prepare the factories and build ViewModels
                    val expenseFactory = ExpenseViewModelFactory(repository, database.expenseDao(), prefs)
                    val expenseViewModel: ExpenseViewModel = viewModel(factory = expenseFactory)

                    val dashboardFactory = DashboardViewModelFactory(database.expenseDao(), prefs)
                    val dashboardViewModel: DashboardViewModel = viewModel(factory = dashboardFactory)

                    //Build the Community ViewModel
                    val communityFactory = CommunityViewModelFactory(repository)
                    val communityViewModel: CommunityViewModel = viewModel(factory = communityFactory)
                    val chatFactory = ChatViewModelFactory(database.expenseDao(), prefs)
                    val chatViewModel: ChatViewModel = viewModel(factory = chatFactory)

                    // 5. Launch the App via NavGraph!
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