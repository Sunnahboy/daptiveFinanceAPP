package com.finadapt.adaptivefinance.core.util

import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryShareCalculatorTest {

    @Test
    fun testEmptyExpenses() {
        val shares = CategoryShareCalculator.calculateTopCategories(emptyList())
        assertEquals(0, shares.size)
    }

    @Test
    fun testCategoryGroupingAndPercentages() {
        val expenses = listOf(
            ExpenseEntity(amount = 100f, category = "Food", timestamp = 1L),
            ExpenseEntity(amount = 50f, category = "Food", timestamp = 2L),
            ExpenseEntity(amount = 300f, category = "Rent", timestamp = 3L),
            ExpenseEntity(amount = 50f, category = "Transport", timestamp = 4L),
            ExpenseEntity(amount = 100f, category = "OtherCat", timestamp = 5L)
        )
        // Total = 600f. Rent = 300 (50%), Food = 150 (25%), OtherCat = 100 (16.67%), Transport = 50 (8.33%)
        // Top 3 are Rent, Food, OtherCat. Transport is grouped into "Other".
        val shares = CategoryShareCalculator.calculateTopCategories(expenses)
        assertEquals(4, shares.size)
        assertEquals("Rent", shares[0].category)
        assertEquals(300f, shares[0].amount)
        assertEquals("Food", shares[1].category)
        assertEquals(150f, shares[1].amount)
        assertEquals("OtherCat", shares[2].category)
        assertEquals("Other", shares[3].category)
        assertEquals(50f, shares[3].amount)
    }
}
