package com.finadapt.adaptivefinance.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.ui.components.LevelUpOverlay
import com.finadapt.adaptivefinance.ui.components.NotificationPermissionHandler
import java.text.SimpleDateFormat
import java.util.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import com.finadapt.adaptivefinance.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    totalSpend: Float,
    monthlyBudget: Float,
    todaySpend: Float,
    currentAiAction: String,
    userXp: Int,
    currentStreak: Int,
    recentExpenses: List<ExpenseEntity>,
    levelUpTier: String?,
    playCoinDrop: Boolean, // 🟢 NEW: Trigger for Lottie
    onAnimationFinished: () -> Unit, // 🟢 NEW: Reset callback
    isDarkMode: Boolean = false,
    onDismissLevelUp: () -> Unit,
    onNavigateToLogExpense: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Check silently in the background
    NotificationPermissionHandler()

    // 🟢 DYNAMIC THEME COLORS
    val gradientStart = Color(0xFF0284C7)
    val gradientEnd = Color(0xFF10B981)
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val strokeBgColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    val isOverBudget = totalSpend > monthlyBudget

    val (insightColor, insightTitle, insightBody, insightIcon) = when {
        isOverBudget -> listOf(Color(0xFFEF4444), "🚨 Budget Exceeded", "You have spent more than your monthly limit. Please stop spending immediately.", Icons.Default.Warning)
        currentAiAction == "strict_budget" -> listOf(Color(0xFFF59E0B), "🔥 Survival Mode", "High volatility detected! The AI recommends pausing non-essential spending.", Icons.Default.Warning)
        currentAiAction == "quiz" -> listOf(Color(0xFF8B5CF6), "🧠 Financial IQ Test", "Review your recent spending habits to earn XP and level up.", Icons.Default.Lightbulb)
        currentAiAction == "cool_off" -> listOf(Color(0xFF06B6D4), "❄️ Cool-Off Period", "Take a deep breath. A 2-hour 'think-before-you-buy' pause is active.", Icons.Default.Schedule)
        currentAiAction == "streak_builder" -> listOf(Color(0xFF3B82F6), "🎯 Streak Builder", "Maintain your streak! Consistent, low spending is key.", Icons.AutoMirrored.Filled.TrendingUp)
        else -> listOf(Color(0xFF10B981), "✨ Great job!", "You're currently within your budget. Keep up the disciplined spending!", Icons.Default.AutoAwesome)
    }

    val currentDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        containerColor = bgColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLogExpense,
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Expense", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // THE GRADIENT HEADER BACKGROUND
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(gradientStart, gradientEnd)),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            )

            // THE SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // --- APP BAR: Greeting & Settings ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("Hey $userName 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Let's stay on budget today.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currentDate, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // V2.0 UNIFIED GAMIFICATION HUB
                UnifiedMascotCard(
                    userName = userName,
                    userXp = userXp,
                    currentStreak = currentStreak,
                    playCoinDrop = playCoinDrop,
                    onAnimationFinished = onAnimationFinished,

                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE OVERLAPPING AI INSIGHT CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = insightColor as Color),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 8.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(insightIcon as ImageVector, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(insightTitle as String, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(insightBody as String, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SPLIT SUMMARY CARDS ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(if (isDarkMode) Color(0xFF0284C7).copy(alpha=0.2f) else Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) {
                                Text("RM", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Today's Spend", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            val formattedTodaySpend = String.format(Locale.getDefault(), "%.2f", todaySpend)
                            Text("RM $formattedTodaySpend", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 18.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(if (isDarkMode) Color(0xFF10B981).copy(alpha=0.2f) else Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("This Month", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            Text("RM ${totalSpend.toInt()}", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE MOOD RING BUDGET CARD ---
                val rawPercentage = if (monthlyBudget > 0f) totalSpend / monthlyBudget else 0f
                val clampedPercentage = rawPercentage.coerceIn(0f, 1f)
                val animatedSweep by animateFloatAsState(
                    targetValue = clampedPercentage,
                    animationSpec = tween(1500, delayMillis = 300),
                    label = "donut_sweep"
                )

                // 1. Determine the Mood & Colors based on spend
                val (ringColor, centerLottieRes, statusText) = when {
                    rawPercentage >= 1f -> Triple(Color(0xFFEF4444), R.raw.status_danger, "☠️ Budget Blown")
                    rawPercentage > 0.6f -> Triple(Color(0xFFF59E0B), R.raw.status_warning, "⚠️ Nearing Limit")
                    else -> Triple(Color(0xFF10B981), R.raw.status_safe, "🛡️ On Track")
                }

                // 2. Load the Lottie for the center
                val centerComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(centerLottieRes))
                val centerProgress by animateLottieCompositionAsState(
                    composition = centerComposition,
                    iterations = LottieConstants.IterateForever // Keeps the mood alive!
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                    border = if (isOverBudget) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side: The Numbers
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusText,
                                color = ringColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RM ${totalSpend.toInt()} / RM ${monthlyBudget.toInt()}",
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOverBudget) "0% Remaining" else "${((1f - clampedPercentage) * 100).toInt()}% Left",
                                fontWeight = FontWeight.Bold,
                                color = subTextColor,
                                fontSize = 14.sp
                            )
                        }

                        // Right Side: The Mood Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(90.dp)
                        ) {
                            // The Compose Background Track
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = strokeBgColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                                // The Compose Colored Progress Fill
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedSweep,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                            }

                            // The Dynamic Center Lottie
                            LottieAnimation(
                                composition = centerComposition,
                                progress = { centerProgress },
                                modifier = Modifier.size(45.dp) // Fits perfectly inside the 90dp donut
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- RECENT TRANSACTIONS ---
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                if (recentExpenses.isEmpty()) {
                    Text("No expenses yet.", color = subTextColor, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    recentExpenses.forEach { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(strokeBgColor, CircleShape), contentAlignment = Alignment.Center) {
                                        Text("🏷️", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(expense.category, fontWeight = FontWeight.Bold, color = textColor)
                                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                }
                                Text("- RM ${expense.amount}", fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (levelUpTier != null) {
                LevelUpOverlay(
                    newTierName = levelUpTier,
                    onDismiss = onDismissLevelUp
                )
            }
        }
    }
}


@Composable
fun UnifiedMascotCard(
    userName: String,
    userXp: Int,
    currentStreak: Int,
    playCoinDrop: Boolean,
    onAnimationFinished: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // 🟢 1. THE STATE MACHINE: Decide WHICH single animation to play
    val currentLottieRes = when {
        playCoinDrop -> R.raw.piggy_feed       // Priority 1: The pig is eating a coin!
        currentStreak == 0 -> R.raw.piggy_broken // Priority 2: Streak broken (Sad pig)
        else -> R.raw.piggy_idle               // Priority 3: Normal breathing pig
    }

    // 🟢 2. Load the chosen animation
    val piggyComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(currentLottieRes))

    // 🟢 3. Control the looping (Feed plays ONCE, others loop forever)
    val shouldLoop = if (playCoinDrop) 1 else LottieConstants.IterateForever

    val piggyProgress by animateLottieCompositionAsState(
        composition = piggyComposition,
        iterations = shouldLoop,
        isPlaying = true
    )

    // Trigger Vibration on feed
    LaunchedEffect(playCoinDrop) {
        if (playCoinDrop) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Reset the state when the Piggy finishes its "Feed" animation!
    LaunchedEffect(piggyProgress) {
        if (playCoinDrop && piggyProgress == 1f) {
            onAnimationFinished()
        }
    }

    // DYNAMIC XP MATH
    val levelName: String
    val tierColor: Color
    val currentLevelMin: Int
    val nextLevelMax: Int

    when {
        userXp < 500 -> {
            levelName = "Bronze Novice"
            tierColor = Color(0xFFD97706)
            currentLevelMin = 0
            nextLevelMax = 500
        }
        userXp < 2000 -> {
            levelName = "Silver Guardian"
            tierColor = Color(0xFF94A3B8)
            currentLevelMin = 500
            nextLevelMax = 2000
        }
        userXp < 5000 -> {
            levelName = "Gold Master"
            tierColor = Color(0xFFF59E0B)
            currentLevelMin = 2000
            nextLevelMax = 5000
        }
        else -> {
            levelName = "Platinum Legend"
            tierColor = Color(0xFF34D399) // Platinum glowing cyan
            currentLevelMin = 5000
            nextLevelMax = userXp // Ring stays permanently full
        }
    }

    // Calculate how full the ring should be for the CURRENT tier
    val rawFillPercentage = if (nextLevelMax > currentLevelMin) {
        (userXp - currentLevelMin).toFloat() / (nextLevelMax - currentLevelMin).toFloat()
    } else {
        1f // If max level, ring is 100% full
    }

    val animatedFill by animateFloatAsState(
        targetValue = rawFillPercentage.coerceIn(0f, 1f),
        animationSpec = tween(1500, delayMillis = 300),
        label = "XP"
    )

    val xpText = if (userXp >= 5000) "$userXp XP (MAX LEVEL)" else "$userXp / $nextLevelMax XP"

    // The Unified Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Deep slate RPG look
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- LEFT SIDE: RPG Stats ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Player: $userName",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = levelName,
                    color = tierColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 🟢 DYNAMIC STREAK PILL (NOW WITH LOTTIE!)
                val isStreakBroken = currentStreak == 0
                val pillBgColor = if (isStreakBroken) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                val streakTextColor = if (isStreakBroken) Color(0xFFFCA5A5) else Color.White

                // Load the Animated Flame or Broken Heart
                val streakLottieRes = if (isStreakBroken) R.raw.streak_broken else R.raw.streak_fire
                val streakComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(streakLottieRes))
                val streakAnimProgress by animateLottieCompositionAsState(
                    composition = streakComposition,
                    iterations = LottieConstants.IterateForever // Keeps it burning!
                )

                Surface(
                    color = pillBgColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 🟢 The Lottie Animation replacing the text emoji!
                        LottieAnimation(
                            composition = streakComposition,
                            progress = { streakAnimProgress },
                            // 1. Force the Lottie to scale up and ignore its internal blank space
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(37.dp) // 2. Cranked up the size!
                                .offset(x = (-4).dp) // 3. Nudges it slightly left so it hugs the text better
                        )

                        // Reduced the spacer from 6.dp to 2.dp because the bigger Lottie takes up more room
                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = "$currentStreak Days",
                            color = streakTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = xpText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // --- RIGHT SIDE: The Mascot & Tight XP Ring ---
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Tight XP Ring
                Canvas(modifier = Modifier.size(110.dp)) {
                    drawArc(
                        color = Color(0xFF334155), // Dark track
                        startAngle = 135f, sweepAngle = 270f, useCenter = false,
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.linearGradient(listOf(Color(0xFF10B981), tierColor)), // Glowing XP matching the Tier Color!
                        startAngle = 135f, sweepAngle = 270f * animatedFill, useCenter = false,
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                }

                // 🟢 The ONE Smart Piggy! No more double layering!
                LottieAnimation(
                    composition = piggyComposition,
                    progress = { piggyProgress },
                    modifier = Modifier.size(85.dp) // Adjusted slightly larger to look great in the ring
                )
            }
        }
    }
}