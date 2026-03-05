package com.finadapt.adaptivefinance.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SlapAnimation(triggerShake: Boolean) {
    // This holds the X-axis position of the icon
    val shakeOffset = remember { Animatable(0f) }

    // When triggerShake becomes true, fire off the violent shake!
    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            // 1. Instantly "pull" the icon 40 pixels to the right
            shakeOffset.snapTo(40f)

            // 2. Release it and let physics handle the friction and decay
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy, // Makes it vibrate rapidly
                    stiffness = Spring.StiffnessHigh // Makes the snap violent and fast
                )
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        // Apply the offset to the Modifier so the whole Box physically shakes
        modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
    ) {
        Text(
            text = "🛑", // The universal "STOP" sign
            fontSize = 80.sp
        )
    }
}