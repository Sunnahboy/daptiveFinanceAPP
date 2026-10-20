package com.finadapt.adaptivefinance.feature.dashboard

import com.finadapt.adaptivefinance.core.util.FinanceFormatting
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardHeaderTest {

    @Test
    fun testStreakChipFormatting() {
        assertEquals("1 day", FinanceFormatting.formatDays(1))
        assertEquals("3 days", FinanceFormatting.formatDays(3))
        assertEquals("10 days", FinanceFormatting.formatDays(10))
    }
}
