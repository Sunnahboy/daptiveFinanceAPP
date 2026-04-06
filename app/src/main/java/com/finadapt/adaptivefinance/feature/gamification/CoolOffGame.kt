
package com.finadapt.adaptivefinance.feature.gamification

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import com.finadapt.adaptivefinance.ui.components.VictoryScreen
import kotlinx.coroutines.delay

enum class CoolOffPhase { INTRO, ACTION, RESULT }

private const val INITIAL_COUNTDOWN = 10

@Composable
fun CoolOffGame(
    awsMessage: String,
    soundEngine: SoundEngine,
    onGameComplete: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    //hardware vibrator for the phone
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val isDark = isSystemInDarkTheme()
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val cyanAccent = Color(0xFF06B6D4)
    val warningRed = Color(0xFFEF4444)

    var currentPhase by remember { mutableStateOf(CoolOffPhase.INTRO) }
    var timeLeft by remember { mutableIntStateOf(INITIAL_COUNTDOWN) }

    var isHolding by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // Load Lotties files
    val zenCompositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.zen_meditate))
    val zenComposition by zenCompositionResult
    val zenProgress by animateLottieCompositionAsState(
        composition = zenComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isHolding
    )

    //LOAD CONFETTI FOR VICTORY
    val confettiCompositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success_confetti))
    val confettiComposition by confettiCompositionResult
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        isPlaying = currentPhase == CoolOffPhase.RESULT,
        iterations = 1
    )

    DisposableEffect(Unit) {
        onDispose { soundEngine.stopAllTicking() }
    }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            soundEngine.play("click")

            while (timeLeft > 0) {
                //Trigger the real hardware vibrator
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    delay(150)
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }

                soundEngine.play("tick2")
                delay(850) //delay to account for the double heartbeat for the vibrator
                timeLeft--
            }

            if (timeLeft == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundEngine.stopAllTicking()
                soundEngine.play("success")
                currentPhase = CoolOffPhase.RESULT
            }
        } else if (hasStarted && timeLeft > 0) {
            soundEngine.stopAllTicking()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("❄️ Zen Mode", fontSize = 24.sp, fontWeight = FontWeight.Black, color = cyanAccent)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {
            CoolOffPhase.INTRO -> {
                val startInteraction = remember { MutableInteractionSource() }
                val isStartPressed by startInteraction.collectIsPressedAsState()

                val skipInteraction = remember { MutableInteractionSource() }
                val isSkipPressed by skipInteraction.collectIsPressedAsState()

                //Taking turns between the lottie files
                var isSmiling by remember { mutableStateOf(true) } //Starts with a smile!

                LaunchedEffect(Unit) {
                    while (true) {
                        isSmiling = true
                        delay(3500) //Smiles for 3.5 seconds
                        isSmiling = false
                        delay(4500) //Rubs chin and thinks for 4.5 seconds
                    }
                }

                //Swaps the Lottie file automatically based on the timer above
                val mascotRes = if (isSmiling) R.raw.mascot_smile else R.raw.mascot_thinking
                val introMascotResult = rememberLottieComposition(LottieCompositionSpec.RawRes(mascotRes))
                val introMascot by introMascotResult
                val mascotProgress by animateLottieCompositionAsState(
                    composition = introMascot,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                //make the lottie float gently
                val floatTransition = rememberInfiniteTransition(label = "float")
                val floatY by floatTransition.animateFloat(
                    initialValue = -10f, //Floats 10 pixels up
                    targetValue = 10f,   //Floats 10 pixels down
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutSine), // Smooth 2-second bob
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "float_anim"
                )

                //The bouncy entrance
                var isMascotVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(150)
                    isMascotVisible = true
                }

                //UI layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    //The mascot
                    AnimatedVisibility(
                        visible = isMascotVisible,
                        enter = androidx.compose.animation.scaleIn(
                            initialScale = 0.3f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        ) + fadeIn()
                    ) {
                        LottieAnimation(
                            composition = introMascot,
                            progress = { mascotProgress },
                            modifier = Modifier
                                .size(110.dp)
                                .offset(y = floatY.dp) //continuous float
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    //The text
                    Text(
                        text = awsMessage,
                        textAlign = TextAlign.Center,
                        color = subTextColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    //Breathing accept button
                    val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
                    val heroScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "pulse_anim"
                    )

                    val startPressScale by animateFloatAsState(targetValue = if (isStartPressed) 0.95f else 1f, animationSpec = tween(100))

                    Button(
                        onClick = {
                            soundEngine.play("click")
                            currentPhase = CoolOffPhase.ACTION
                        },
                        interactionSource = startInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = cyanAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(heroScale * startPressScale)
                    ) {
                        Text("I am a Zen Master 🧘‍♂️✨", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    //Skip button
                    val skipScale by animateFloatAsState(targetValue = if (isSkipPressed) 0.8f else 1f, animationSpec = tween(150))

                    TextButton(
                        onClick = {
                            soundEngine.play("lose")
                            onGameComplete(0)
                        },
                        interactionSource = skipInteraction,
                        modifier = Modifier.scale(skipScale)
                    ) {
                        Text(
                            text = "Nah, I lack discipline 📉",
                            color = warningRed.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            CoolOffPhase.ACTION -> {
                val instructionText = when {
                    !hasStarted -> "Press and hold to breathe."
                    isHolding -> "Keep holding. Clearing mind..."
                    else -> "Focus lost! Place your thumb back."
                }
                val instructionColor = if (!isHolding && hasStarted) warningRed else subTextColor

                Text(instructionText, textAlign = TextAlign.Center, color = instructionColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = zenCompositionResult.isComplete,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    LottieAnimation(
                        composition = zenComposition,
                        progress = { zenProgress },
                        modifier = Modifier.size(150.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val scannerGlow by animateFloatAsState(
                    targetValue = if (isHolding) 1.2f else 1f,
                    animationSpec = tween(500)
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scannerGlow)
                        .clip(CircleShape)
                        .background(if (isHolding) cyanAccent.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 4.dp,
                            color = if (isHolding) cyanAccent else if (hasStarted) warningRed else cyanAccent.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                isHolding = true
                                hasStarted = true
                                waitForUpOrCancellation()
                                isHolding = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Hold to Meditate",
                        tint = if (isHolding) cyanAccent else if (hasStarted) warningRed else subTextColor,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "$timeLeft s",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isHolding) cyanAccent else if (hasStarted) warningRed else subTextColor
                )
            }

            //VICTORY SCREEN
            CoolOffPhase.RESULT -> {
                Box(contentAlignment = Alignment.Center) {
                    //Confetti explosion behind the victory screen
                    this@Column.AnimatedVisibility(
                        visible = confettiCompositionResult.isComplete,
                        enter = fadeIn(animationSpec = tween(300))
                    ) {
                        LottieAnimation(
                            composition = confettiComposition,
                            progress = { confettiProgress },
                            modifier = Modifier.size(300.dp).offset(y = (-50).dp)
                        )
                    }

                    VictoryScreen(
                        title = "Mind Cleared!",
                        xpEarned = 50,
                        onDismiss = { onGameComplete(1) }
                    )
                }
            }
        }
    }
}