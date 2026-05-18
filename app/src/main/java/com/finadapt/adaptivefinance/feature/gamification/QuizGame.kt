
package com.finadapt.adaptivefinance.feature.gamification

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import kotlinx.coroutines.delay

enum class QuizPhase { INTRO, PLAYING, RESULT }

val financialTrivia = listOf(
    Pair("What is the 50/30/20 budget rule?", listOf("Needs/Wants/Savings", "Savings/Needs/Wants", "Wants/Savings/Needs")),
    Pair("Which category drains a budget fastest due to 'invisible' spending?", listOf("Dining Out & Subscriptions", "Rent & Utilities", "Car Insurance")),
    Pair("What is an emergency fund typically supposed to cover?", listOf("3-6 months of expenses", "1 month of expenses", "A down payment"))
)

private const val QUIZ_TIME_LIMIT = 10 //10 SECONDS FOR HIGH STAKES!

@Composable
fun QuizGame(
    awsMessage: String,
    soundEngine: SoundEngine,
    onGameComplete: (Int) -> Unit
) {
    val context = LocalContext.current


    //HARDWARE VIBRATOR
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
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val brandPurple = Color(0xFFA78BFA)
    val warningOrange = Color(0xFFF97316)

    var currentPhase by remember { mutableStateOf(QuizPhase.INTRO) }
    var currentQuestion by remember { mutableStateOf(financialTrivia.random()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    val correctAnswer = currentQuestion.second[0]
    val shuffledOptions by remember { mutableStateOf(currentQuestion.second.shuffled()) }

    var timeLeft by remember { mutableIntStateOf(QUIZ_TIME_LIMIT) }
    var isTimeUp by remember { mutableStateOf(false) }

    // Smooth Burning Fuse Progress
    val animatedProgress by animateFloatAsState(
        targetValue = timeLeft.toFloat() / QUIZ_TIME_LIMIT,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "fuse"
    )

    // Lottie Loaders for Result Phase
    DisposableEffect(Unit) { onDispose { soundEngine.stopAllTicking() } }

    //THE TIMER LOOP
    LaunchedEffect(currentPhase) {
        if (currentPhase == QuizPhase.PLAYING) {
            timeLeft = QUIZ_TIME_LIMIT
            while (timeLeft > 0) {
                delay(1000)
                if (currentPhase != QuizPhase.PLAYING || selectedAnswer != null) break
                timeLeft--
                when (timeLeft) {
                    in 1..3 -> soundEngine.play("tick1") // Fast ticking!
                }
            }

            if (timeLeft == 0 && selectedAnswer == null) {
                isTimeUp = true
                soundEngine.stopAllTicking()

                // Heavy buzz for running out of time
                if (vibrator.hasVibrator()) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }

                soundEngine.play("lose")
                currentPhase = QuizPhase.RESULT
            }
        }
    }

    //3.THE 3D FLIP DELAY MANAGER
    //When they click an answer, we wait 1.2 seconds to let the card physically flip before changing screens
    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            soundEngine.stopAllTicking()
            val isCorrect = selectedAnswer == correctAnswer

            if (isCorrect) {
                soundEngine.play("success")
            } else {
                soundEngine.play("lose")
                if (vibrator.hasVibrator()) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 50, 200), -1) // Error buzz pattern
                }
            }
            delay(1200) // Wait for the 3D flip to finish!
            currentPhase = QuizPhase.RESULT
        }
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("\uD83E\uDDE0 Trivia Duel", fontSize = 24.sp, fontWeight = FontWeight.Black, color = brandPurple)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {
            //ALIVE & GLOWING INTRO
            QuizPhase.INTRO -> {
                val startInteraction = remember { MutableInteractionSource() }
                val isStartPressed by startInteraction.collectIsPressedAsState()

                val skipInteraction = remember { MutableInteractionSource() }
                val isSkipPressed by skipInteraction.collectIsPressedAsState()

                //1. The Brain! (Swaps every 3 seconds)
                var isThinking by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    while (true) {
                        isThinking = true
                        delay(3500)
                        isThinking = false
                        delay(3500)
                    }
                }

                val targetLottieRes = if (isThinking) R.raw.quiz_intro else R.raw.quiz_ready
                val introLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(targetLottieRes))
                val introLottie by introLottieResult
                val mascotProgress by animateLottieCompositionAsState(
                    composition = introLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                //2. The Gentle Floating Physics
                val floatTransition = rememberInfiniteTransition(label = "float")
                val floatY by floatTransition.animateFloat(
                    initialValue = -15f,
                    targetValue = 15f,
                    animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "float_anim"
                )

                //3. The Hologram Breathing Glow
                val glowAlpha by floatTransition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "glow_anim"
                )

                var isMascotVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(150)
                    isMascotVisible = true
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    //THE HOLOGRAPHIC MASCOT
                    AnimatedVisibility(
                        visible = isMascotVisible,
                        enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            //The ambient glow behind the brain
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .offset(y = floatY.dp)
                                    .background(
                                        Brush.radialGradient(listOf(brandPurple.copy(alpha = glowAlpha), Color.Transparent)),
                                        shape = RoundedCornerShape(100)
                                    )
                            )

                            //The Brain Lottie
                            LottieAnimation(
                                composition = introLottie,
                                progress = { mascotProgress },
                                modifier = Modifier
                                    .size(160.dp)
                                    .offset(y = floatY.dp)
                            )
                        }
                    }

                    Text(
                        text = awsMessage,
                        textAlign = TextAlign.Center,
                        color = Color.White, // High contrast
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold //Bolder, more confident text
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // THE BREATHING HERO BUTTON
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 1f, targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                        label = "scale"
                    )
                    val startScale by animateFloatAsState(targetValue = if (isStartPressed) 0.95f else 1f, animationSpec = tween(100))

                    Button(
                        onClick = {
                            soundEngine.play("click")
                            currentPhase = QuizPhase.PLAYING
                        },
                        interactionSource = startInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = brandPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).scale(pulse * startScale)
                    ) {
                        Text("I'm a Financial Genius 🤓", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                    }

                    //SKIP BUTTON
                    val skipScale by animateFloatAsState(targetValue = if (isSkipPressed) 0.8f else 1f)

                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.1f), // Subtle red pill background
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.scale(skipScale)
                    ) {
                        TextButton(
                            onClick = {
                                soundEngine.play("lose")
                                onGameComplete(0)
                            },
                            interactionSource = skipInteraction
                        ) {
                            Text(
                                text = "I want to stay broke 💸",
                                color = Color(0xFFFCA5A5), //Brighter pastel red for dark mode!
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            //IMMERSIVE HIGH-STAKES PLAYING (Lottie Inside Question Card!)
            QuizPhase.PLAYING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "playing_pulse")
                val bgGlow by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.7f,

                    animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "bg_glow"
                )

                val isPanicMode = timeLeft <= 3
                val rawShake by infiniteTransition.animateFloat(
                    initialValue = -6f, targetValue = 6f,
                    animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
                    label = "shake"
                )
                val currentShakeX = if (isPanicMode) rawShake.dp else 0.dp

                //Load the loading/thinking mascot
                val playingLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.quiz_loading))
                val playingLottie by playingLottieResult
                val playingProgress by animateLottieCompositionAsState(
                    composition = playingLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

                    //1. THE HOLOGRAPHIC BACKGROUND LAYER
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = 1.5f
                                scaleY = 1.5f
                                alpha = bgGlow
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = if (isPanicMode) listOf(Color(0xFFEF4444).copy(alpha = 0.3f), Color.Transparent)
                                    else listOf(brandPurple.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                    )

                    //2. THE FOREGROUND UI
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {

                        //THE BURNING FUSE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(subTextColor.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = animatedProgress)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = if (!isPanicMode) listOf(brandPurple, Color(0xFFC4B5FD)) else listOf(Color.Red, warningOrange)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // THE SHAKING TIMER
                        Text(
                            text = "00:${timeLeft.toString().padStart(2, '0')}",
                            color = if (!isPanicMode) subTextColor else Color.Red,
                            fontWeight = FontWeight.Black,
                            fontSize = if (!isPanicMode) 22.sp else 28.sp,
                            modifier = Modifier.offset(x = currentShakeX)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        //QUESTION CARD
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)), // Made slightly darker for contrast
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, brandPurple.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                //MASCOT WRAPPER
                                Box(
                                    modifier = Modifier
                                        .size(76.dp) //ensure it isn't crushed
                                        //ensures DARK Lotties don't blend into the navy background!
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(100)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (playingLottie != null) {
                                        LottieAnimation(
                                            composition = playingLottie,
                                            progress = { playingProgress },
                                            modifier = Modifier.fillMaxSize().padding(4.dp), //Fills the box perfectly
                                            contentScale = ContentScale.Crop //Ignores massive blank canvas space
                                        )
                                    } else {
                                        //IF  SPINNING FOREVER,  FILE IS MISSING,
                                        CircularProgressIndicator(
                                            color = brandPurple,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = currentQuestion.first,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        //THE "CASINO DEALER" STAGGERED CARDS
                        shuffledOptions.forEachIndexed { index, option ->
                            var isCardVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(index * 150L)
                                isCardVisible = true
                            }

                            AnimatedVisibility(
                                visible = isCardVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { 100 },
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                ) + fadeIn()
                            ) {
                                FlipAnswerCard(
                                    text = option,
                                    isRevealed = selectedAnswer == option,
                                    isCorrect = option == correctAnswer,
                                    isEnabled = selectedAnswer == null,
                                    onClick = { selectedAnswer = option }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }


            //EXPLOSIVE RESULT (Now with 3 Emotional Mascot States!)
            QuizPhase.RESULT -> {
                val isCorrect = selectedAnswer == correctAnswer

                // 1. The 3-Way Emotion Brain!
                val targetLottieRes = when {
                    isTimeUp -> R.raw.quiz_timeout   //Ran out of time
                    isCorrect -> R.raw.quiz_success  //Got it right
                    else -> R.raw.quiz_fail          //Got it wrong
                }

                //Load ONLY the correct emotion dynamically
                val resultLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(targetLottieRes))
                val resultComposition by resultLottieResult

                //I set this to IterateForever so the mascot keeps smiling or crying while the user reads the text!
                val resultProgress by animateLottieCompositionAsState(
                    composition = resultComposition,
                    isPlaying = true,
                    iterations = LottieConstants.IterateForever
                )

                // The Liveness Trick: Delay the text so the Mascot pops first!
                var showDetails by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(300)
                    showDetails = true
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // 2. THE EMOTIONAL MASCOT
                    AnimatedVisibility(
                        visible = resultLottieResult.isComplete,
                        enter = scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        ) + fadeIn()
                    ) {
                        LottieAnimation(
                            composition = resultComposition,
                            progress = { resultProgress },
                            modifier = Modifier.size(150.dp) // Perfect size, won't cover text!
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. THE DETAILS (Slides up smoothly)
                    AnimatedVisibility(
                        visible = showDetails,
                        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            // Dynamic Text Content
                            if (isTimeUp) {
                                Text("⏳ Too Slow!", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.Red)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("You fell asleep at the wheel.", color = subTextColor)
                            } else if (isCorrect) {
                                Text("Brilliant! \uD83D\uDE0D", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFF10B981))
                                Spacer(modifier = Modifier.height(16.dp))
                                // XP Badge
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "💎 +50 XP",
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text("Ouch! \uD83D\uDE2D", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.Red)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("The right answer was:", color = subTextColor, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(correctAnswer, textAlign = TextAlign.Center, color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            //The Bouncy Return Button
                            val btnInteraction = remember { MutableInteractionSource() }
                            val isBtnPressed by btnInteraction.collectIsPressedAsState()
                            val btnScale by animateFloatAsState(targetValue = if (isBtnPressed) 0.95f else 1f, animationSpec = tween(100))

                            val btnColor = if (isCorrect && !isTimeUp) Color(0xFF10B981) else Color(0xFF64748B)

                            Button(
                                onClick = { onGameComplete(if (isCorrect && !isTimeUp) 1 else 0) },
                                interactionSource = btnInteraction,
                                colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                                modifier = Modifier.fillMaxWidth().height(56.dp).scale(btnScale),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (isCorrect && !isTimeUp) "Claim XP & Return" else "Better luck next time",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

//4. THE CUSTOM 3D FLIP CARD
@Composable
fun FlipAnswerCard(
    text: String,
    isRevealed: Boolean,
    isCorrect: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    //Math for the 3D Rotation
    val rotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "flip"
    )

    // If it has rotated past 90 degrees,  looking at the "back" of the card
    val isBackVisible = rotation > 90f

    val cardBgColor = if (!isBackVisible) Color(0xFF334155) else if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density // Adds 3D perspective depth!
            },
        colors = CardDefaults.cardColors(containerColor = cardBgColor, disabledContainerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isRevealed) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!isBackVisible) {
                // Front of Card
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
            } else {
                // Back of Card (Needs to be flipped -180 degrees so the text isn't backwards)
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                    Text(
                        text = if (isCorrect) "\uD83D\uDC4A Correct!" else "❌ Wrong!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}