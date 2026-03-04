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
import com.finadapt.adaptivefinance.core.navigation.Screen
import com.finadapt.adaptivefinance.data.local.AppDatabase
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModel
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModelFactory
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModel
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the Room Database & Prefs
        val database = AppDatabase.getDatabase(applicationContext)
        val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)

        // ⚠️ UNCOMMENT THIS LINE ONLY WHEN YOU WANT TO FORCE RESET THE APP FOR TESTING
        // prefs.edit { clear() }

        // 2. Give BOTH the Database and Prefs to the repository
        val repository = FinanceRepository(database.expenseDao(), prefs)

        setContent {
            // Standard Material 3 Wrapper
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 3. Prepare the factories and build ViewModels
                    val expenseFactory = ExpenseViewModelFactory(repository,prefs)
                    val expenseViewModel: ExpenseViewModel = viewModel(factory = expenseFactory)

                    val dashboardFactory = DashboardViewModelFactory(database.expenseDao(), prefs)
                    val dashboardViewModel: DashboardViewModel = viewModel(factory = dashboardFactory)

                    // 4. SMART ROUTING: Check if they are a returning user
                    val hasCompletedOnboarding = prefs.getString("SILENT_USER_ID", null) != null
                    val startDestination = if (hasCompletedOnboarding) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }

                    // 5. Launch the App via NavGraph!
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        dashboardViewModel = dashboardViewModel,
                        expenseViewModel = expenseViewModel,
                        prefs = prefs // Pass prefs to fetch the UUID in the graph
                    )
                }
            }
        }
    }
}