package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import com.finadapt.adaptivefinance.ui.components.VictoryScreen
import kotlinx.coroutines.delay

// 🟢 1. The 3 Phases of the Cool-Off Intervention
enum class CoolOffPhase { INTRO, ACTION, RESULT }

// 🟢 CONSTANTS - Configurable values
private const val INITIAL_COUNTDOWN = 10  // 🔴 INCREASED from 5 to 10 seconds
private const val BREATHING_CYCLE_MS = 2500  // 2.5s inhale, 2.5s exhale (total 5s per cycle)

@Composable
fun CoolOffGame(
    awsMessage: String,
    soundEngine: SoundEngine, // 🟢 Inject Sound Engine
    onGameComplete: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val cyanAccent = Color(0xFF06B6D4)

    var currentPhase by remember { mutableStateOf(CoolOffPhase.INTRO) }
    var timeLeft by remember { mutableIntStateOf(INITIAL_COUNTDOWN) }

    //Kills audio if the user force-closes the app or dialog
    DisposableEffect(Unit) {
        onDispose {
            soundEngine.stopAllTicking()
        }
    }

    // 🟢 The Timer Logic with Audio Tension (ENHANCED!)
    LaunchedEffect(currentPhase) {
        if (currentPhase == CoolOffPhase.ACTION) {
            timeLeft = INITIAL_COUNTDOWN  // Reset timer

            while (timeLeft > 0) {
                // 🔴 FIXED: Ticking sound NOW PLAYS during breathing!
                soundEngine.play("tick2")

                delay(1000)

                // 🛑 BREAKOUT: If they somehow skip/cancel, stop the timer!
                if (currentPhase != CoolOffPhase.ACTION) {
                    soundEngine.stopAllTicking()
                    break
                }

                timeLeft--
            }

            // When timer hits zero naturally:
            if (currentPhase == CoolOffPhase.ACTION && timeLeft == 0) {
                soundEngine.stopAllTicking() // 🛑 KILL TICKING
                soundEngine.play("success")
                currentPhase = CoolOffPhase.RESULT
            }
        }
    }


    // 🟢 The "Breathing" Math (INCREASED: Now 5 seconds per inhale/exhale cycle)
    // Customize BREATHING_CYCLE_MS to change speed:
    // - 2500ms = 2.5s inhale + 2.5s exhale (original, faster)
    // - 3000ms = 3s inhale + 3s exhale (slower, more calming)
    // - 4000ms = 4s inhale + 4s exhale (very slow, deep breathing)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(BREATHING_CYCLE_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("❄️ Cool-Off Period", fontSize = 24.sp, fontWeight = FontWeight.Black, color = cyanAccent)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {

            // 🟢 PHASE 1: The Choice
            CoolOffPhase.INTRO -> {
                Text(awsMessage, textAlign = TextAlign.Center, color = subTextColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            soundEngine.stopAllTicking()
                            soundEngine.play("lose") // Play fail sound
                            onGameComplete(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip", color = subTextColor)
                    }
                    Button(
                        onClick = {
                            soundEngine.play("click") // Tactile click to start
                            currentPhase = CoolOffPhase.ACTION
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Timeout", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 🟢 PHASE 2: The Forced Friction (WITH TICKING AUDIO + LONGER BREATHING)
            CoolOffPhase.ACTION -> {
                Text("Breathe in... Breathe out...", textAlign = TextAlign.Center, color = subTextColor)
                Spacer(modifier = Modifier.height(48.dp))

                // The Animated Breathing Circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .background(cyanAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$timeLeft",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = cyanAccent
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }

            // 🟢 PHASE 3: The Reward
            CoolOffPhase.RESULT -> {
                // We use your new reusable component!
                VictoryScreen(
                    title = "Mind Cleared!",
                    xpEarned = 50,
                    onDismiss = {
                        onGameComplete(1) // This sends the +1 to AWS and closes the popup!
                    }
                )
            }
        }
    }
}