package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.finadapt.adaptivefinance.ui.components.AscensionAnimation
import com.finadapt.adaptivefinance.ui.components.SoundEngine

// We use the same 3-phase system from the Quiz!
enum class StreakPhase { INTRO, PLAYING, RESULT }

@Composable
fun StreakBuilderGame(
    awsMessage: String,
    soundEngine: SoundEngine, // 🟢 Inject Sound Engine
    onGameComplete: (Int) -> Unit
) {
    val brandBlue = Color(0xFF3B82F6)
    var currentPhase by remember { mutableStateOf(StreakPhase.INTRO) }
    var tapsRemaining by remember { mutableIntStateOf(3) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

        Text("🎯 Streak Builder", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = brandBlue)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {

            // 🟢 PHASE 1: The Custom Intro ("Claim Bonus")
            StreakPhase.INTRO -> {
                Text(awsMessage, textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            soundEngine.play("lose") // Play fail sound
                            onGameComplete(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip", color = Color.Gray)
                    }
                    Button(
                        onClick = { currentPhase = StreakPhase.PLAYING },
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Custom phrasing!
                        Text("Claim Bonus", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 🟢 PHASE 2: The Action
            StreakPhase.PLAYING -> {
                Box(modifier = Modifier.height(120.dp), contentAlignment = Alignment.Center) {
                    AscensionAnimation()
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        soundEngine.play("click") // Play click sound on every tap!
                        if (tapsRemaining > 1) {
                            tapsRemaining--
                        } else {
                            soundEngine.play("success") // WIN SOUND!
                            currentPhase = StreakPhase.RESULT
                        }
                    },
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                    modifier = Modifier.size(120.dp).graphicsLayer { scaleX = buttonScale; scaleY = buttonScale },
                    shape = CircleShape
                ) {
                    Text(text = "TAP x$tapsRemaining", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tap fast to break the box!", color = Color.Gray, fontSize = 12.sp)
            }

            // 🟢 PHASE 3: The Result
            StreakPhase.RESULT -> {
                Text("Bonus Claimed! +50 XP", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onGameComplete(1) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}