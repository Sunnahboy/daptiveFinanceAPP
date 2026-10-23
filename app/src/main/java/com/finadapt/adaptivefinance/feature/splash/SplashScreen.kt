package com.finadapt.adaptivefinance.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.finadapt.adaptivefinance.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isReady: Boolean,
    onSplashFinished: () -> Unit
) {
    // Pre-load core dashboard Lotties into memory cache during the splash window
    // so they are 100% parsed and ready to render instantly when the app opens.
    rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.streak_fire))
    rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.mascot_smile))
    rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.piggy_idle))

    val infiniteTransition = rememberInfiniteTransition(label = "splash_transition")

    // Continuous rotation for the loading ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Dynamic transition illusion: phase 1 (ring only) -> phase 2 (fade in branding)
    var showBranding by remember { mutableStateOf(false) }
    val textAlpha by animateFloatAsState(
        targetValue = if (showBranding) 1f else 0f,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "branding_fade"
    )

    // Pulsing alpha for the loading text
    val loadingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_pulse"
    )

    // Trigger phase 2 transition illusion almost instantly (50ms) so branding is visible right away
    LaunchedEffect(Unit) {
        delay(50L)
        showBranding = true
    }

    LaunchedEffect(isReady) {
        if (isReady) {
            // Brief graceful hold once data is fully ready
            delay(500L)
            onSplashFinished()
        }
    }

    // Single seamless screen with deep dark background and status bar padding to prevent layout shifting
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B10))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating multi-color loading ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
            ) {
                val strokeWidth = 16.dp.toPx()

                // Segment 1: Emerald (#10B981)
                drawArc(
                    color = Color(0xFF10B981),
                    startAngle = 0f,
                    sweepAngle = 130f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Segment 2: Blue (#3B82F6)
                drawArc(
                    color = Color(0xFF3B82F6),
                    startAngle = 145f,
                    sweepAngle = 80f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Segment 3: Purple (#8B5CF6)
                drawArc(
                    color = Color(0xFF8B5CF6),
                    startAngle = 240f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Segment 4: Amber (#F59E0B)
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = 325f,
                    sweepAngle = 25f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center Branding & Loading Status (fades in smoothly in phase 2)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(
                    text = "FinAdapt",
                    color = Color.White.copy(alpha = textAlpha),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Loading insights...",
                    color = Color(0xFF94A3B8).copy(alpha = loadingAlpha * textAlpha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
