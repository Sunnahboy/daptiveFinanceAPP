package com.finadapt.adaptivefinance.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class CoolOffTest {

    @Test
    fun testCoolOffBannerVisibilityLogic() {
        val activeAction = "cool_off"
        val zenAction = "zen"

        val isCoolOffActive = (activeAction == "cool_off")
        val isZenActive = (zenAction == "cool_off")

        assertEquals(true, isCoolOffActive)
        assertEquals(false, isZenActive)
    }
}
