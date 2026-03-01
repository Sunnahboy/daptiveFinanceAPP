package com.finadapt.adaptivefinance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finadapt.adaptivefinance.data.local.AppDatabase
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.expense.AddExpenseScreen
import com.finadapt.adaptivefinance.feature.expense.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //1. Initialize the Room Database
        val database = AppDatabase.getDatabase(applicationContext)

        //2. Give the Database to the repository
        val repository = FinanceRepository(database.expenseDao())

        //3.Prepare the factory to build viewModel
        val factory = ExpenseViewModelFactory(repository)

        setContent {
            // Standard Material 3 Wrapper
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 4. Inject the factory into the UI
                    AddExpenseScreen(viewModel(factory  = factory))
                }
            }
        }
    }
}