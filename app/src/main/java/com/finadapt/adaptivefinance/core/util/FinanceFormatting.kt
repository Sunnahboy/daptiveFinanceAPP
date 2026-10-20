package com.finadapt.adaptivefinance.core.util

import java.util.Locale

object FinanceFormatting {
    fun formatCurrency(amount: Float, currencySymbol: String): String {
        val cleanSymbol = currencySymbol.ifBlank { "RM" }
        return if (amount % 1f == 0f) {
            String.format(Locale.getDefault(), "%s %,d", cleanSymbol, amount.toLong())
        } else {
            String.format(Locale.getDefault(), "%s %,.2f", cleanSymbol, amount)
        }
    }

    fun formatDays(days: Int): String {
        return if (days == 1) "1 day" else "$days days"
    }
}
