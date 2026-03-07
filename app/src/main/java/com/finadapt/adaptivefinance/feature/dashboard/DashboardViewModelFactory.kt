package com.finadapt.adaptivefinance.feature.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.repository.FinanceRepository

class DashboardViewModelFactory(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val financeRepository = FinanceRepository(expenseDao, prefs)
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(expenseDao, prefs, financeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}