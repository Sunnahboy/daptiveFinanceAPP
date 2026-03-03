package com.finadapt.adaptivefinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finadapt.adaptivefinance.data.local.AppDatabase
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.expense.AddExpenseScreen
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModelFactory
import com.finadapt.adaptivefinance.feature.onboarding.OnboardingScreen
import androidx.core.content.edit
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1.Initialize the Room Database
        val database = AppDatabase.getDatabase(applicationContext)

        //Get SharedPreferences
        val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)

        // 2.Give BOTH the Database and Prefs to the repository
        val repository = FinanceRepository(database.expenseDao(), prefs)

        // 3.Prepare the factory to build viewModel
        val factory = ExpenseViewModelFactory(repository)

        setContent {
            // Standard Material 3 Wrapper
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // 1 check if the user already has a UUID saved
                    val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)
                    prefs.edit { clear() }//for testing clears users id
                    val hasCompletedOnboarding  = prefs.getString("SILENT_USER_ID", null) != null

                    //2 Remember that state si the UI knows which screen to sho
                    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding) }

                        // 3. Navigation Logic
                    if(showOnboarding){
                        //show the beautiful new flow
                        OnboardingScreen(
                            onFinish =  {showOnboarding = false} // when done,  switch to Dashboard

                        )
                    }else{
                        //Get the viewModel and state
                        val expenseViewModel: ExpenseViewModel = viewModel(factory = factory)
                        val uiState by expenseViewModel.uiState.collectAsState()
                        //Grab the real UUID from SharedPreferences
                        val prefs = applicationContext.getSharedPreferences("AdaptiveFinancePrefs", MODE_PRIVATE)
                        // (use dummy UUID as a fallback just in case)
                        val realUserId = prefs.getString("SILENT_USER_ID", "123e4567-e89b-12d3-a456-426614174000")
                            ?: "123e4567-e89b-12d3-a456-426614174000"

                        AddExpenseScreen(
                            uiState = uiState,
                            onLogExpense = { amount, category ->
                                expenseViewModel.submitExpense(amount, category,realUserId)
                            },
                            onFeedback = { predictionId, reward ->
                                expenseViewModel.submitFeedback(predictionId, reward)
                            },
                            onDismissState = {
                                // Currently ViewModel doesn't have a reset function,
                                // let the sheet close for now!
                                expenseViewModel.resetState()
                            }
                        )

                    }



                }
            }
        }
    }
}