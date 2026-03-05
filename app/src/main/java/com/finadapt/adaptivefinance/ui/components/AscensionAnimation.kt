package com.finadapt.adaptivefinance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AscensionAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "ascension")

    // 🟢 1. Float Upward Animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    // 🟢 2. Fade In & Out Animation (Keyframes!)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                0f at 0         // Start invisible
                1f at 500       // Fully visible at 500ms
                0f at 1500      // Fade out as it reaches the top
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // 🟢 3. The UI Element
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = "🔥",
            fontSize = 64.sp,
            modifier = Modifier
                .offset(y = offsetY.dp)
                .alpha(alpha)
        )
    }
}