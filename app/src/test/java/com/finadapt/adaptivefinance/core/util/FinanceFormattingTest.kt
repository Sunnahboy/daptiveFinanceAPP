package com.finadapt.adaptivefinance.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceFormattingTest {

    @Test
    fun testFormatCurrencyWithDifferentSymbols() {
        assertEquals("KSh 1,430", FinanceFormatting.formatCurrency(1430f, "KSh"))
        assertEquals("RM 50", FinanceFormatting.formatCurrency(50f, "RM"))
        assertEquals("$ 70.50", FinanceFormatting.formatCurrency(70.5f, "$"))
    }

    @Test
    fun testFormatDaysPluralization() {
        assertEquals("1 day", FinanceFormatting.formatDays(1))
        assertEquals("5 days", FinanceFormatting.formatDays(5))
        assertEquals("0 days", FinanceFormatting.formatDays(0))
    }
}
