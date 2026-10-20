package com.finadapt.adaptivefinance.core.util

import com.finadapt.adaptivefinance.data.local.ExpenseEntity

data class CategoryShare(
    val category: String,
    val amount: Float,
    val percentage: Float
)

object CategoryShareCalculator {
    fun calculateTopCategories(expenses: List<ExpenseEntity>): List<CategoryShare> {
        if (expenses.isEmpty()) return emptyList()
        val total = expenses.sumOf { it.amount.toDouble() }.toFloat()
        if (total <= 0f) return emptyList()

        val grouped = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
            .entries.sortedByDescending { it.value }

        val result = mutableListOf<CategoryShare>()
        val top = grouped.take(3)

        top.forEach { entry ->
            result.add(CategoryShare(entry.key, entry.value, (entry.value / total) * 100f))
        }

        val rest = grouped.drop(3)
        if (rest.isNotEmpty()) {
            val restSum = rest.sumOf { it.value.toDouble() }.toFloat()
            if (restSum > 0f) {
                result.add(CategoryShare("Other", restSum, (restSum / total) * 100f))
            }
        }
        return result
    }
}
