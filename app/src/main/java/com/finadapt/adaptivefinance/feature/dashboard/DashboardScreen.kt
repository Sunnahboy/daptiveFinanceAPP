
package com.finadapt.adaptivefinance.feature.dashboard

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.feature.gamification.GamificationDialog // 🟢 NEW IMPORT
import com.finadapt.adaptivefinance.ui.components.LevelUpOverlay
import com.finadapt.adaptivefinance.ui.components.NotificationPermissionHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    totalSpend: Float,
    monthlyBudget: Float,
    todaySpend: Float,
    currentAiAction: String,
    currentStreak: Int,
    recentExpenses: List<ExpenseEntity>,
    levelUpTier: String?,
    playCoinDrop: Boolean,
    onAnimationFinished: () -> Unit,
    isDarkMode: Boolean = false,
    onDismissLevelUp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    levelName: String,
    tierColorHex: Long,
    fillPercentage: Float,
    xpText: String,
    //send game feedback to the server from the Dashboard
    onGameFeedback: (String, String, Boolean) -> Unit
) {
    NotificationPermissionHandler()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)

    // 🟢 DYNAMIC THEME COLORS
    val gradientStart = Color(0xFF0284C7)
    val gradientEnd = Color(0xFF10B981)
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val strokeBgColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

    // 🟢 AMBUSH DETECTOR STATES
    var pendingAmbushAction by remember { mutableStateOf<String?>(null) }
    var pendingAmbushMessage by remember { mutableStateOf<String?>(null) }
    var pendingAmbushId by remember { mutableStateOf<String?>(null) }

    // Check for an ambush the moment the screen loads
    LaunchedEffect(Unit) {
        val action = prefs.getString("PENDING_AMBUSH_ACTION", null)
        if (action != null) {
            pendingAmbushAction = action
            pendingAmbushMessage = prefs.getString("PENDING_AMBUSH_MESSAGE", "Time for a challenge!")
            pendingAmbushId = prefs.getString("PENDING_AMBUSH_ID", "")
        }
    }

    // 🟢 RECEIPT VIEWER STATES
    var selectedExpenseForDetails by remember { mutableStateOf<ExpenseEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // THE GRADIENT HEADER BACKGROUND
            Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(brush = Brush.linearGradient(colors = listOf(gradientStart, gradientEnd)), shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)))

            // THE SCROLLABLE CONTENT
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(48.dp))

                // --- APP BAR ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
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

                UnifiedMascotCard(
                    userName = userName,
                    currentStreak = currentStreak,
                    playCoinDrop = playCoinDrop,
                    onAnimationFinished = onAnimationFinished,
                    levelName = levelName,
                    tierColorHex = tierColorHex,
                    fillPercentage = fillPercentage,
                    xpText = xpText
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE OVERLAPPING AI INSIGHT CARD ---
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = insightColor as Color), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 8.dp)) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(insightIcon as ImageVector, contentDescription = null, tint = Color.White) }
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
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(if (isDarkMode) Color(0xFF0284C7).copy(alpha=0.2f) else Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) { Text("RM", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold) }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Today's Spend", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            val formattedTodaySpend = String.format(Locale.getDefault(), "%.2f", todaySpend)
                            Text("RM $formattedTodaySpend", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 18.sp)
                        }
                    }

                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(if (isDarkMode) Color(0xFF10B981).copy(alpha=0.2f) else Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp)) }
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
                val animatedSweep by animateFloatAsState(targetValue = clampedPercentage, animationSpec = tween(1500, delayMillis = 300), label = "donut_sweep")

                val (ringColor, centerLottieRes, statusText) = when {
                    rawPercentage >= 1f -> Triple(Color(0xFFEF4444), R.raw.status_danger, "☠️ Budget Blown")
                    rawPercentage > 0.6f -> Triple(Color(0xFFF59E0B), R.raw.status_warning, "⚠️ Nearing Limit")
                    else -> Triple(Color(0xFF10B981), R.raw.status_safe, "🛡️ On Track")
                }

                val centerComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(centerLottieRes))
                val centerProgress by animateLottieCompositionAsState(composition = centerComposition, iterations = LottieConstants.IterateForever)

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp), border = if (isOverBudget) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = statusText, color = ringColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "RM ${totalSpend.toInt()} / RM ${monthlyBudget.toInt()}", fontWeight = FontWeight.Black, color = textColor, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = if (isOverBudget) "0% Remaining" else "${((1f - clampedPercentage) * 100).toInt()}% Left", fontWeight = FontWeight.Bold, color = subTextColor, fontSize = 14.sp)
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(color = strokeBgColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 24f, cap = StrokeCap.Round))
                                drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * animatedSweep, useCenter = false, style = Stroke(width = 24f, cap = StrokeCap.Round))
                            }
                            LottieAnimation(composition = centerComposition, progress = { centerProgress }, modifier = Modifier.size(45.dp))
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
                        val hasReceipt = expense.receiptImagePath.isNotEmpty() || expense.items.isNotEmpty()

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = hasReceipt) { selectedExpenseForDetails = expense },
                            colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 1.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(40.dp).background(strokeBgColor, CircleShape), contentAlignment = Alignment.Center) { Text("🏷️", fontSize = 16.sp) }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(expense.merchantName.ifEmpty { expense.category }, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasReceipt) { Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Has Receipt", tint = subTextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)) }
                                    Text("- RM ${String.format(Locale.US, "%.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = textColor)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }



            if (levelUpTier != null) {
                LevelUpOverlay(newTierName = levelUpTier, onDismiss = onDismissLevelUp)
            }

            // 🟢 THE AMBUSH UI (Pops up if they have a pending game)
            if (pendingAmbushAction != null) {
                GamificationDialog(
                    action = pendingAmbushAction!!,
                    message = pendingAmbushMessage!!,
                    predictionId = pendingAmbushId!!,
                    onFeedback = { predId, rewardInt ->
                        // 1. Send the feedback to the server
                        onGameFeedback(predId, pendingAmbushAction!!, rewardInt == 1)

                        // 2. Clear the ambush from memory so it doesn't pop up again!
                        prefs.edit { remove("PENDING_AMBUSH_ACTION") }
                        pendingAmbushAction = null
                    },
                    onDismiss = {
                        // User skipped/finished the game, clear it.
                        prefs.edit { remove("PENDING_AMBUSH_ACTION")}
                        pendingAmbushAction = null
                    }
                )
            }

            // --- BOTTOM SHEET RECEIPT VIEWER ---
            if (selectedExpenseForDetails != null) {
                val expense = selectedExpenseForDetails!!
                val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

                ModalBottomSheet(onDismissRequest = { selectedExpenseForDetails = null }, sheetState = sheetState, containerColor = cardColor) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.merchantName.ifEmpty { expense.category }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                                Text(expense.date.ifEmpty { dateFormatter.format(Date(expense.timestamp)) }, color = subTextColor, fontSize = 14.sp)
                            }

                            if (expense.receiptImagePath.isNotEmpty()) {
                                var showFullImage by remember { mutableStateOf(false) }
                                AsyncImage(model = File(expense.receiptImagePath), contentDescription = "Receipt Thumbnail", contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).clickable { showFullImage = true })
                                if (showFullImage) {
                                    Dialog(onDismissRequest = { showFullImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
                                            AsyncImage(model = File(expense.receiptImagePath), contentDescription = "Full Receipt", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                            IconButton(onClick = { showFullImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        if (expense.items.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(expense.items) { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1)
                                            Text(item.category, fontSize = 12.sp, color = Color(0xFF0284C7))
                                        }
                                        Text("RM ${String.format(Locale.US, "%.2f", item.amount)}", fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = subTextColor.copy(alpha = 0.1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Text("This expense was logged manually without itemized details.", color = subTextColor, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("RM ${String.format(Locale.US, "%.2f", expense.amount)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = gradientStart)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// (UnifiedMascotCard function remains identical to your previous code)
@Composable
fun UnifiedMascotCard(
    userName: String,
    currentStreak: Int,
    playCoinDrop: Boolean,
    onAnimationFinished: () -> Unit,
    //come strictly from the ViewModel now
    levelName: String,
    tierColorHex: Long,
    fillPercentage: Float,
    xpText: String
) {
    val haptic = LocalHapticFeedback.current
    val tierColor = Color(tierColorHex) // Convert the Long back into a Compose Color

    // --- LOTTIE ANIMATION LOGIC (Stays the same) ---
    val currentLottieRes = when {
        playCoinDrop -> R.raw.piggy_feed
        currentStreak == 0 -> R.raw.piggy_broken
        else -> R.raw.piggy_idle
    }

    val piggyCompositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(currentLottieRes))
    val piggyComposition by piggyCompositionResult

    val shouldLoop = if (playCoinDrop) 1 else LottieConstants.IterateForever
    val piggyProgress by animateLottieCompositionAsState(
        composition = piggyComposition,
        iterations = shouldLoop,
        isPlaying = true
    )

    val isStreakBroken = currentStreak == 0
    val streakLottieRes = if (isStreakBroken) R.raw.streak_broken else R.raw.streak_fire

    val streakCompositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(streakLottieRes))
    val streakComposition by streakCompositionResult

    val streakAnimProgress by animateLottieCompositionAsState(
        composition = streakComposition,
        iterations = LottieConstants.IterateForever
    )

    LaunchedEffect(playCoinDrop) {
        if (playCoinDrop) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(piggyProgress) {
        if (playCoinDrop && piggyProgress == 1f) {
            onAnimationFinished()
        }
    }

    // --- ANIMATED PROGRESS BAR LOGIC ---
    val animatedFill by animateFloatAsState(
        targetValue = fillPercentage, // Uses the exact math from the ViewModel!
        animationSpec = tween(1500, delayMillis = 300),
        label = "XP"
    )

    // --- UI RENDERING ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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

                val pillBgColor = if (isStreakBroken) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                val streakTextColor = if (isStreakBroken) Color(0xFFFCA5A5) else Color.White

                Surface(
                    color = pillBgColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = streakCompositionResult.isComplete,
                            enter = fadeIn(animationSpec = tween(500))
                        ) {
                            LottieAnimation(
                                composition = streakComposition,
                                progress = { streakAnimProgress },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .offset(x = (-4).dp)
                            )
                        }

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

            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = 135f, sweepAngle = 270f, useCenter = false,
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.linearGradient(listOf(Color(0xFF10B981), tierColor)),
                        startAngle = 135f, sweepAngle = 270f * animatedFill, useCenter = false,
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )
                }

                this@Row.AnimatedVisibility(
                    visible = piggyCompositionResult.isComplete,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    LottieAnimation(
                        composition = piggyComposition,
                        progress = { piggyProgress },
                        modifier = Modifier.size(85.dp)
                    )
                }
            }
        }
    }
}