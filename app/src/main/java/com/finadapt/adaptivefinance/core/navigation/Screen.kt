package com.finadapt.adaptivefinance.core.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object AddExpense : Screen("add_expense")

    object History : Screen("history")
    object Settings : Screen("settings")
    object Rewards : Screen("rewards")
    object Community : Screen("community")
}


