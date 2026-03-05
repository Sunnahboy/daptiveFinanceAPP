package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.ui.components.SlapAnimation
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import com.finadapt.adaptivefinance.ui.components.VictoryScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// 🟢 1. The 3 Phases of the Survival Mode
enum class StrictPhase { INTRO, ACTION, RESULT }

@Composable
fun StrictBudgetGame(
    awsMessage: String,
    soundEngine: SoundEngine, // 🟢 Inject Sound Engine
    onGameComplete: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val warningRed = Color(0xFFEF4444)

    var currentPhase by remember { mutableStateOf(StrictPhase.INTRO) }
    var triggerShake by remember { mutableStateOf(false) }

    //SAFETY NET for the warning rumble
    DisposableEffect(Unit) {
        onDispose {
            soundEngine.stop("warning")
        }
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔥 Survival Mode", fontSize = 24.sp, fontWeight = FontWeight.Black, color = warningRed)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {

            // 🟢 PHASE 1: The Warning Intro
            StrictPhase.INTRO -> {
                Text(awsMessage, textAlign = TextAlign.Center, color = subTextColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            soundEngine.stop("warning")
                            soundEngine.play("lose") // Play failure sound
                            onGameComplete(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ignore Warning", color = subTextColor)
                    }
                    Button(
                        onClick = {
                            // 🔊 Play the heavy jolt sound!
                            soundEngine.play("warning")
                            triggerShake = true
                            currentPhase = StrictPhase.ACTION
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = warningRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review Alert", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 🟢 PHASE 2: The Physical Friction
            StrictPhase.ACTION -> {
                // The Native Visual "Slap"
                Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.Center) {
                    SlapAnimation(triggerShake = triggerShake)
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("You must physically lock your budget to continue.", textAlign = TextAlign.Center, color = subTextColor)
                Spacer(modifier = Modifier.height(32.dp))

                // The Slider
                SwipeToConfirmLock(
                    soundEngine = soundEngine,
                    onConfirm = {
                        soundEngine.stop("warning")
                        soundEngine.play("success") // 🔊 Victory sound!
                        currentPhase = StrictPhase.RESULT
                    }
                )
            }

            // 🟢 PHASE 3: The Reward
            // 🟢 PHASE 3: The Reward
            StrictPhase.RESULT -> {
                VictoryScreen(
                    title = "Budget Locked!",
                    xpEarned = 50,
                    onDismiss = { onGameComplete(1) }
                )
            }
        }
    }
}

// 🟢 Custom Premium Component: Gesture-driven Slide to Lock
@Composable
fun SwipeToConfirmLock(soundEngine: SoundEngine, onConfirm: () -> Unit) {
    val width = 280.dp
    val thumbSize = 56.dp

    val density = LocalDensity.current
    val maxSwipePx = with(density) { (width - thumbSize).toPx() }
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()
    val trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .width(width)
            .height(thumbSize)
            .background(trackColor, CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Background Track Text
        Text(
            text = "Slide to Lock Budget",
            color = if (isDark) Color.Gray else Color.DarkGray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // The Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(thumbSize - 8.dp)
                .background(Color(0xFFEF4444), CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // 🔊 Play mechanical click when they grab the slider!
                            soundEngine.play("click")
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                // If they swiped more than 75% of the way, complete the action
                                if (dragOffset.value > maxSwipePx * 0.75f) {
                                    dragOffset.animateTo(maxSwipePx)
                                    onConfirm()
                                } else {
                                    // Otherwise, snap it back to the start (Friction!)
                                    soundEngine.play("click") // 🔊 Play click again as it snaps back
                                    dragOffset.animateTo(0f)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxSwipePx)
                            dragOffset.snapTo(newOffset)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Swipe", tint = Color.White)
        }
    }
}