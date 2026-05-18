


package com.finadapt.adaptivefinance.feature.gamification

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.CheckCircle


enum class StrictPhase { INTRO, ACTION, RESULT }

@Composable
fun StrictBudgetGame(
    awsMessage: String,
    soundEngine: SoundEngine,
    onGameComplete: (Int) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // High Industrial Color Palette
    val darkMetal = Color(0xFF0F172A)
    val brushedSteel = Color(0xFF1E293B)
    val neonCyan = Color(0xFF06B6D4)
    val alertAmber = Color(0xFFF59E0B)
    val dangerRed = Color(0xFFEF4444)

    var currentPhase by remember { mutableStateOf(StrictPhase.INTRO) }

    // Lock States
    var lock1 by remember { mutableStateOf(false) }
    var lock2 by remember { mutableStateOf(false) }
    var lock3 by remember { mutableStateOf(false) }

    // Watch for all 3 locks being engaged
    LaunchedEffect(lock1, lock2, lock3) {
        if (lock1 && lock2 && lock3) {
            soundEngine.play("success")
            delay(500) // Brief pause to admire the fully locked UI
            currentPhase = StrictPhase.RESULT
        }
    }

    DisposableEffect(Unit) { onDispose { soundEngine.stop("warning") } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(darkMetal)
            .animateContentSize(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "Security", tint = alertAmber, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("High-Risk Mode", fontSize = 24.sp, fontWeight = FontWeight.Black, color = alertAmber, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        when (currentPhase) {
            StrictPhase.INTRO -> {
                // 1. Load the "Consequence" Lottie (Burning money, empty wallet, etc.)
                val dangerLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.danger_broke))
                val dangerLottie by dangerLottieResult
                val dangerProgress by animateLottieCompositionAsState(
                    composition = dangerLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                // 2. High-Class Animations (Pulsing Glow & Sweeping Button Shimmer)
                val infiniteTransition = rememberInfiniteTransition(label = "intro_anims")

                // The glowing red alarm behind the Lottie
                val threatGlow by infiniteTransition.animateFloat(
                    initialValue = 0.1f, targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "threat_glow"
                )

                // The Premium Fintech "Shimmer" for the primary button
                val shimmerTranslate by infiniteTransition.animateFloat(
                    initialValue = -500f, targetValue = 1000f,
                    animationSpec = infiniteRepeatable(tween(2500, delayMillis = 500, easing = LinearEasing)),
                    label = "shimmer"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // THE THREAT HOLOGRAM (Shows them going broke!)
                    Box(contentAlignment = Alignment.Center) {
                        // Pulsing red alarm background
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .background(
                                    Brush.radialGradient(listOf(dangerRed.copy(alpha = threatGlow), Color.Transparent)),
                                    shape = RoundedCornerShape(100)
                                )
                        )

                        if (dangerLottie != null) {
                            LottieAnimation(
                                composition = dangerLottie,
                                progress = { dangerProgress },
                                modifier = Modifier.size(160.dp)
                            )
                        }
                    }

                    // THE THREAT READOUT TERMINAL
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = brushedSteel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, dangerRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚠️ SEVERE RISK DETECTED", color = dangerRed, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = awsMessage.uppercase(),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // THE HIGH-CLASS ANIMATED BUTTONS
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {

                    val startInteraction = remember { MutableInteractionSource() }
                    val isStartPressed by startInteraction.collectIsPressedAsState()
                    val startScale by animateFloatAsState(targetValue = if (isStartPressed) 0.95f else 1f, animationSpec = tween(100))

                    // 1. The Shimmering "Initiate" Button
                    Button(
                        onClick = {
                            soundEngine.play("warning")
                            currentPhase = StrictPhase.ACTION
                        },
                        interactionSource = startInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan.copy(alpha = 0.2f)), // Glassy Cyan Base
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth().height(64.dp).scale(startScale)
                    ) {
                        // We build the button background manually to apply the sweeping shimmer effect
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    //Using 2D Offset points for the shimmer trajectory
                                    Brush.linearGradient(
                                        colors = listOf(neonCyan.copy(alpha = 0.4f), neonCyan, neonCyan.copy(alpha = 0.4f)),
                                        start = Offset(x = shimmerTranslate, y = 0f),
                                        end = Offset(x = shimmerTranslate + 400f, y = 0f)
                                    )
                                )
                                .border(1.dp, neonCyan, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = darkMetal)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ENGAGE VAULT", fontWeight = FontWeight.Black, color = darkMetal, fontSize = 18.sp, letterSpacing = 2.sp)
                            }
                        }
                    }

                    // 2. The Unstable "Abort" Button
                    val skipInteraction = remember { MutableInteractionSource() }
                    val isSkipPressed by skipInteraction.collectIsPressedAsState()
                    val skipScale by animateFloatAsState(targetValue = if (isSkipPressed) 0.95f else 1f)

                    // A subtle shake to make the abort button look like a bad idea
                    val glitchShake by infiniteTransition.animateFloat(
                        initialValue = -2f, targetValue = 2f,
                        animationSpec = infiniteRepeatable(tween(50, delayMillis = 3000, easing = LinearEasing), RepeatMode.Reverse),
                        label = "glitch"
                    )

                    OutlinedButton(
                        onClick = {
                            soundEngine.play("lose")
                            onGameComplete(0)
                        },
                        interactionSource = skipInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(skipScale)
                            .offset(x = glitchShake.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text("LEAVE UNPROTECTED", color = dangerRed.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            //PHASE 2: THE DEADBOLT HUD
            StrictPhase.ACTION -> {
                val scanLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.industrial_scan))
                val scanLottie by scanLottieResult
                val scanProgress by animateLottieCompositionAsState(
                    composition = scanLottie,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )

                //the Live Terminal Typist Effect
                val terminalLogs = listOf(
                    "> SECURING VOLATILE FUNDS...",
                    "> REROUTING TO VAULT PROTOCOL...",
                    "> ENCRYPTING SPENDING LIMITS...",
                    "> WAITING FOR MANUAL CYLINDER OVERRIDE...",
                    "> SYSTEM STABLE. AWAITING LOCKDOWN."
                )
                var currentLogIndex by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1200)
                        currentLogIndex = (currentLogIndex + 1) % terminalLogs.size
                    }
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

                    if (scanLottie != null) {
                        LottieAnimation(
                            composition = scanLottie,
                            progress = { scanProgress },
                            modifier = Modifier
                                .size(300.dp) // Made slightly bigger to fill space
                                .offset(y = (-40).dp) // Shifted up slightly
                                .graphicsLayer { alpha = 0.15f },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ENGAGE SECURITY CYLINDERS", color = neonCyan, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(32.dp))

                        // THE 3 SLEEK DEADBOLTS
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // Tighter spacing (16dp)
                            HeavyDeadbolt(label = "CYLINDER 01", isLocked = lock1, vibrator = vibrator, soundEngine = soundEngine) { lock1 = true }
                            HeavyDeadbolt(label = "CYLINDER 02", isLocked = lock2, vibrator = vibrator, soundEngine = soundEngine) { lock2 = true }
                            HeavyDeadbolt(label = "CYLINDER 03", isLocked = lock3, vibrator = vibrator, soundEngine = soundEngine) { lock3 = true }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        //The Terminal Output Box (Fills the bottom space
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .border(1.dp, neonCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = terminalLogs[currentLogIndex],
                                color = neonCyan.copy(alpha = 0.8f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            //  PHASE 3: THE VAULT SLAM (Single Master Card
            StrictPhase.RESULT -> {
                // 1. Pre-load Vault Lottie
                val slamLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.vault_slam))
                val slamLottie by slamLottieResult
                val slamProgress by animateLottieCompositionAsState(
                    composition = slamLottie,
                    isPlaying = true,
                    iterations = 1
                )

                val isSecured = slamProgress > 0.85f

                // 2. Pre-load Success Lottie (Starts exactly when the vault shuts)
                val successLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.shield_success))
                val successLottie by successLottieResult
                val successProgress by animateLottieCompositionAsState(
                    composition = successLottie,
                    iterations = 1,
                    isPlaying = isSecured
                )

                // 3. Fast-Shrink Math for the Vault
                val vaultSize by animateDpAsState(
                    targetValue = if (isSecured) 130.dp else 260.dp, // Shrinks instantly when shut!
                    animationSpec = tween(250, easing = FastOutSlowInEasing), // Fast, snappy mechanical shrink
                    label = "vault_shrink"
                )

                LaunchedEffect(slamLottieResult.isComplete) {
                    if (vibrator.hasVibrator()) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 100, 50, 400), -1)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    //  THE SINGLE MASTER CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = brushedSteel),
                        // The border instantly lights up Cyan when secured!
                        border = androidx.compose.foundation.BorderStroke(2.dp, if (isSecured) neonCyan else Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp).fillMaxWidth()
                        ) {

                            // THE SHRINKING VAULT
                            if (slamLottie != null) {
                                LottieAnimation(
                                    composition = slamLottie,
                                    progress = { slamProgress },
                                    modifier = Modifier.size(vaultSize),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }


                            if (isSecured) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // The sleek Success Lottie (Plays instantly)
                                if (successLottie != null) {
                                    LottieAnimation(
                                        composition = successLottie,
                                        progress = { successProgress },
                                        modifier = Modifier.size(60.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Text(
                                    text = "BUDGET SECURED",
                                    color = neonCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // High-Tech XP Badge
                                Box(
                                    modifier = Modifier
                                        .background(darkMetal, RoundedCornerShape(4.dp))
                                        .border(1.dp, neonCyan, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text("💎 +100 XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }

                                Spacer(modifier = Modifier.height(24.dp))


                                // continuous sweeping shimmer so it screams "CLICK ME!"
                                val infiniteTransition = rememberInfiniteTransition(label = "btn_shimmer")
                                val shimmerTranslate by infiniteTransition.animateFloat(
                                    initialValue = -500f, targetValue = 1000f,
                                    animationSpec = infiniteRepeatable(tween(2000, delayMillis = 500, easing = LinearEasing)),
                                    label = "shimmer"
                                )

                                Button(
                                    onClick = { onGameComplete(1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // Handled by the Box below
                                    contentPadding = PaddingValues(0.dp), // Removes default padding so the background fills perfectly
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp) // slightly taller for a better tap target!
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                // The sweeping light effect!
                                                Brush.linearGradient(
                                                    colors = listOf(neonCyan, Color(0xFFE0F2FE), neonCyan), // Bright cyan -> White -> Bright Cyan
                                                    start = Offset(shimmerTranslate, 0f),
                                                    end = Offset(shimmerTranslate + 300f, 0f)
                                                )
                                            )
                                            // A sharp, bright border to make it pop off the dark background
                                            .border(2.dp, neonCyan.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Added a bold success icon!
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = darkMetal,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "SYSTEM LOCKDOWN COMPLETE",
                                                color = darkMetal,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//CUSTOM INDUSTRIAL COMPONENT: THE HEAVY DEADBOLT
@Composable
fun HeavyDeadbolt(
    label: String,
    isLocked: Boolean,
    vibrator: Vibrator,
    soundEngine: SoundEngine,
    onLock: () -> Unit
) {
    val density = LocalDensity.current
    val trackWidth = 280.dp
    val thumbSize = 52.dp
    val maxSwipePx = with(density) { (trackWidth - thumbSize).toPx() }

    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val trackColor = if (isLocked) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color(0xFF1E293B)
    val thumbColor = if (isLocked) Color(0xFF06B6D4) else Color(0xFF475569)

    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(thumbSize)
            .clip(RoundedCornerShape(8.dp))
            .background(trackColor)
            .border(2.dp, if (isLocked) Color(0xFF06B6D4) else Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Label Text
        Text(
            text = if (isLocked) "LOCKED" else label,
            color = if (isLocked) Color(0xFF06B6D4) else Color.Gray,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // The Heavy Steel Slider
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(thumbSize - 8.dp)
                .background(
                    Brush.verticalGradient(listOf(thumbColor, thumbColor.copy(alpha = 0.7f))),
                    RoundedCornerShape(6.dp)
                )
                .pointerInput(isLocked) { // Disable input if already locked
                    if (isLocked) return@pointerInput

                    detectHorizontalDragGestures(
                        onDragStart = { soundEngine.play("click") },
                        onDragEnd = {
                            coroutineScope.launch {
                                // Must drag 85% of the way to lock it(Heavy friction)
                                if (dragOffset.value > maxSwipePx * 0.85f) {
                                    dragOffset.animateTo(maxSwipePx, animationSpec = spring(stiffness = Spring.StiffnessMedium))

                                    if (vibrator.hasVibrator()) {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(100)
                                    }
                                    soundEngine.play("click")
                                    onLock()
                                } else {
                                    if (vibrator.hasVibrator()) {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(50)
                                    }
                                    dragOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
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
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isLocked) Color.White else Color(0xFF94A3B8)
            )

        }

    }
}