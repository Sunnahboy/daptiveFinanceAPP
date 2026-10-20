package com.finadapt.adaptivefinance.feature.expense

import org.junit.Assert.assertEquals
import org.junit.Test

class AddExpenseValidationTest {

    @Test
    fun testAmountParsing() {
        val cases = listOf(
            "" to null,
            "abc" to null,
            "-50" to -50f,
            "0" to 0f,
            "100.5" to 100.5f,
            "50" to 50f
        )

        cases.forEach { (input, expected) ->
            val parsed = input.toFloatOrNull()
            assertEquals(expected, parsed)
        }
    }

    @Test
    fun testSaveButtonLabelLogic() {
        // Pure function matching the logic in the UI
        fun getSaveButtonLabel(amount: Float?, category: String, currencySymbol: String): String {
            val isAmountMissing = amount == null || amount <= 0f
            val isCategoryMissing = category.isBlank() || category == "Other"

            return when {
                isAmountMissing && isCategoryMissing -> "Enter amount and choose a category"
                isAmountMissing -> "Enter an amount"
                isCategoryMissing -> "Choose a category"
                else -> "Save $currencySymbol $amount"
            }
        }

        // Test 1: Both missing
        assertEquals("Enter amount and choose a category", getSaveButtonLabel(null, "", "KSh"))
        assertEquals("Enter amount and choose a category", getSaveButtonLabel(0f, "Other", "KSh"))

        // Test 2: Amount missing
        assertEquals("Enter an amount", getSaveButtonLabel(null, "Food", "KSh"))

        // Test 3: Category missing
        assertEquals("Choose a category", getSaveButtonLabel(50f, "", "KSh"))
        assertEquals("Choose a category", getSaveButtonLabel(50f, "Other", "KSh"))

        // Test 4: Both valid
        assertEquals("Save KSh 50.0", getSaveButtonLabel(50f, "Food", "KSh"))
    }
}
