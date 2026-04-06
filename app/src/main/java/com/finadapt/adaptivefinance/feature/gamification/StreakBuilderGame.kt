
package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import kotlinx.coroutines.delay

enum class StreakPhase { INTRO, PLAYING, RESULT }

@Composable
fun StreakBuilderGame(
    awsMessage: String,
    soundEngine: SoundEngine,
    onGameComplete: (Int) -> Unit
) {
    val brandBlue = Color(0xFF3B82F6)
    val neonGold = Color(0xFFF59E0B)
    val darkBg = Color(0xFF0F172A)

    var currentPhase by remember { mutableStateOf(StreakPhase.INTRO) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            .background(darkBg, RoundedCornerShape(16.dp))
            .border(1.dp, brandBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-Tech Header
        Text("🔥 DAILY FORGE", fontSize = 20.sp, fontWeight = FontWeight.Black, color = neonGold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))

        when (currentPhase) {
            // 🟢 PHASE 1: PREMIUM INTRO
            StreakPhase.INTRO -> {
                val introLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.streak_intro))
                val introLottie by introLottieResult
                val introProgress by animateLottieCompositionAsState(
                    composition = introLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Box(modifier = Modifier.size(100.dp).background(Brush.radialGradient(listOf(brandBlue.copy(alpha = 0.3f), Color.Transparent)), CircleShape))
                    if (introLottie != null) {
                        LottieAnimation(composition = introLottie, progress = { introProgress }, modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = awsMessage.uppercase(),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "btn_shimmer")
                val shimmerTranslate by infiniteTransition.animateFloat(
                    initialValue = -500f, targetValue = 1000f,
                    animationSpec = infiniteRepeatable(tween(2500, delayMillis = 500, easing = LinearEasing)),
                    label = "shimmer"
                )

                Button(
                    onClick = {
                        soundEngine.play("warning")
                        currentPhase = StreakPhase.PLAYING
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(brandBlue.copy(alpha = 0.6f), brandBlue, brandBlue.copy(alpha = 0.6f)),
                                    start = androidx.compose.ui.geometry.Offset(shimmerTranslate, 0f),
                                    end = androidx.compose.ui.geometry.Offset(shimmerTranslate + 300f, 0f)
                                )
                            )
                            .border(1.dp, brandBlue, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("INITIATE CHARGE SEQUENCE", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                    }
                }
                // 🟢 ADDED: THE DECLINE BUTTON FOR THE BANDIT (Sends 0)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        soundEngine.play("lose") // A subtle negative sound
                        onGameComplete(0) // 🟢 Crucial for the Contextual Bandit!
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)) // Dark metal border
                ) {
                    Text("DECLINE DAILY FORGE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // 🟢 PHASE 2: THE REACTIVE RAPID TAP MINIGAME
            StreakPhase.PLAYING -> {
                var chargeLevel by remember { mutableFloatStateOf(0f) }
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // Smoothly animates the UI bar
                val animatedCharge by animateFloatAsState(
                    targetValue = chargeLevel,
                    animationSpec = tween(100),
                    label = "charge_bar"
                )

                // 🟢 CONSTANT DRAIN MECHANIC
                LaunchedEffect(currentPhase) {
                    while (currentPhase == StreakPhase.PLAYING) {
                        delay(50) // Checks incredibly fast
                        if (chargeLevel > 0f && !isPressed) {
                            // Drains rapidly if they aren't tapping the button!
                            chargeLevel = (chargeLevel - 0.015f).coerceAtLeast(0f)
                        }
                    }
                }

                // Load Lottie
                val chargeLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.core_charge))
                val chargeLottie by chargeLottieResult

                Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    if (chargeLottie != null) {
                        // 🟢 FIXED: The Lottie frame is now physically tied to the battery percentage!
                        LottieAnimation(
                            composition = chargeLottie,
                            progress = { animatedCharge },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Text updates instantly
                    Text("${(animatedCharge * 100).toInt()}%", color = neonGold, fontWeight = FontWeight.Black, fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // The Progress Bar Container
                Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape).background(Color(0xFF1E293B))) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedCharge.coerceAtLeast(0.001f)) // Prevents Compose 0f crash
                            .background(Brush.horizontalGradient(listOf(brandBlue, neonGold)))
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 🟢 THE NEW PUMP ACTION LOTTIE FOR THE BUTTON
                val pumpLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.pump_action))
                val pumpLottie by pumpLottieResult
                val pumpProgress by animateLottieCompositionAsState(
                    composition = pumpLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                // The Big Rapid Tap Button
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                Button(
                    onClick = {
                        soundEngine.play("click")
                        chargeLevel = (chargeLevel + 0.15f).coerceAtMost(1f)

                        if (chargeLevel >= 0.99f) {
                            soundEngine.play("success")
                            currentPhase = StreakPhase.RESULT
                        }
                    },
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(containerColor = neonGold),
                    modifier = Modifier.fillMaxWidth().height(72.dp).scale(buttonScale),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    // 🟢 Put the Lottie and Text together in a Row!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (pumpLottie != null) {
                            LottieAnimation(
                                composition = pumpLottie,
                                progress = { pumpProgress },
                                modifier = Modifier.size(40.dp) // Perfectly sized for the button!
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Text(
                            text = "PUMP POWER!",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = darkBg,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // 🟢 PHASE 3: THE EXPLOSIVE RESULT
            StreakPhase.RESULT -> {
                val flameLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.streak_flame))
                val flameLottie by flameLottieResult
                val flameProgress by animateLottieCompositionAsState(composition = flameLottie, iterations = LottieConstants.IterateForever, isPlaying = true)

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (flameLottie != null) {
                        LottieAnimation(composition = flameLottie, progress = { flameProgress }, modifier = Modifier.size(180.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("CORE IGNITED! 🔥", fontWeight = FontWeight.Black, fontSize = 24.sp, color = neonGold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Consistency is your superpower.", color = Color.LightGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.background(brandBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).border(1.dp, brandBlue, RoundedCornerShape(8.dp)).padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("💎 +50 XP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onGameComplete(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CLAIM STREAK & RETURN", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}