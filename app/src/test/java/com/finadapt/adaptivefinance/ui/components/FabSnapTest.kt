package com.finadapt.adaptivefinance.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FabSnapTest {

    @Test
    fun testFabClampingLogic() {
        val minOffset = -300f
        val maxOffset = 300f

        val rawOffset1 = 450f
        val clamped1 = rawOffset1.coerceIn(minOffset, maxOffset)
        assertEquals(300f, clamped1)

        val rawOffset2 = -500f
        val clamped2 = rawOffset2.coerceIn(minOffset, maxOffset)
        assertEquals(-300f, clamped2)
    }
}
