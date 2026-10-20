
package com.finadapt.adaptivefinance.feature.dashboard

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.feature.gamification.GamificationDialog
import com.finadapt.adaptivefinance.ui.components.LevelUpOverlay
import com.finadapt.adaptivefinance.ui.components.NotificationPermissionHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    userName: String,
    currencySymbol: String,
    totalSpend: Float,
    monthlyBudget: Float,
    todaySpend: Float,
    currentAiAction: String,
    currentStreak: Int,
    recentExpenses: List<ExpenseEntity>,
    allExpenses: List<ExpenseEntity> = emptyList(),
    levelUpTier: String?,
    playCoinDrop: Boolean,
    onAnimationFinished: () -> Unit,
    onDismissLevelUp: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onDeleteExpense: (ExpenseEntity) -> Unit = {},
    levelName: String,
    tierColorHex: Long,
    fillPercentage: Float,
    xpText: String,
    onGameFeedback: (String, String, Boolean) -> Unit
) {
    NotificationPermissionHandler()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)

    //DYNAMIC THEME COLORS
    val gradientStart = MaterialTheme.colorScheme.primary
    val gradientEnd = MaterialTheme.colorScheme.secondary
    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strokeBgColor = MaterialTheme.colorScheme.surfaceVariant

    var pendingAmbushAction by remember { mutableStateOf<String?>(null) }
    var pendingAmbushMessage by remember { mutableStateOf<String?>(null) }
    var pendingAmbushId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val action = prefs.getString("PENDING_AMBUSH_ACTION", null)
        if (action != null) {
            pendingAmbushAction = action
            pendingAmbushMessage = prefs.getString("PENDING_AMBUSH_MESSAGE", "Time for a challenge!")
            pendingAmbushId = prefs.getString("PENDING_AMBUSH_ID", "")
        }
    }

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

    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .statusBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                val dynamicTitle by dashboardViewModel.dynamicTitle
                val dynamicSubtitle by dashboardViewModel.dynamicSubtitle

                val timeGreeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    in 17..21 -> "Good evening"
                    else -> "Hey"
                }

            // 1. COMPACT HEADER ROW (56-64px)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Greeting on the left
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$timeGreeting, $userName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showStreakSheet by remember { mutableStateOf(false) }

                    // Streak Chip
                    val isStreakBroken = currentStreak == 0
                    val streakLottieRes = if (isStreakBroken) R.raw.streak_broken else R.raw.streak_fire
                    val streakCompositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(streakLottieRes))
                    val streakComposition by streakCompositionResult
                    val streakAnimProgress by animateLottieCompositionAsState(
                        composition = streakComposition,
                        iterations = LottieConstants.IterateForever
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        onClick = { showStreakSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    modifier = Modifier.size(16.dp).offset(x = (-2).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatDays(currentStreak),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (showStreakSheet) {
                        ModalBottomSheet(onDismissRequest = { showStreakSheet = false }, containerColor = cardColor) {
                            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                                Text(
                                    text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatDays(currentStreak) + " streak",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Log an expense each day to keep it going.",
                                    color = subTextColor,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                                    val calendar = Calendar.getInstance()
                                    // Make Sunday 7, Monday 1
                                    val currentDayOfWeek = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

                                    days.forEachIndexed { index, dayName ->
                                        // Highlight the current day of the week if streak > 0
                                        val isCurrentDay = (index + 1) == currentDayOfWeek
                                        val isStreakActive = currentStreak > 0

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        if (isCurrentDay && isStreakActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCurrentDay && isStreakActive) {
                                                    Text("🔥", fontSize = 16.sp)
                                                } else {
                                                    Text("-", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = dayName,
                                                color = if (isCurrentDay && isStreakActive) MaterialTheme.colorScheme.onBackground else subTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = { showStreakSheet = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Close", color = MaterialTheme.colorScheme.onBackground)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // Circular Profile Avatar (Initials Fallback) -> Opens Settings
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onNavigateToSettings() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName.take(1).uppercase().ifEmpty { "U" },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. AI INSIGHT CARD (Moved to top per feedback)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dynamicSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. UNIFIED MASCOT CARD
            UnifiedMascotCard(
                userName = userName,
                playCoinDrop = playCoinDrop,
                onAnimationFinished = onAnimationFinished,
                levelName = levelName,
                tierColorHex = tierColorHex,
                fillPercentage = fillPercentage,
                xpText = xpText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. THE AI ACTION / COOL-OFF BANNER
            var showCoolOffSheet by remember { mutableStateOf(false) }
            if (currentAiAction == "cool_off") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCoolOffSheet = true },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF06B6D4).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("❄️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cool-Off Period Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Text("A 2-hour thinking pause is active. Tap for options.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                            }
                        }
                        Text(">", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showCoolOffSheet) {
                    ModalBottomSheet(onDismissRequest = { showCoolOffSheet = false }, containerColor = cardColor) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Cool-Off Pause Active", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Take a deep breath. This cool-off pause encourages mindful spending before making non-essential purchases.", color = subTextColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCoolOffSheet = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                            ) {
                                Text("Keep the Pause", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = {
                                    prefs.edit { putString("LAST_AI_ACTION", "zen") }
                                    showCoolOffSheet = false
                                    dashboardViewModel.loadDashboardData()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Skip Pause", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(24.dp))

                // SHEET STATES FOR INTERACTIVITY
                var showBudgetSheet by remember { mutableStateOf(false) }
                var showCategorySheet by remember { mutableStateOf(false) }

                // 4. HERO BUDGET CARD & CATEGORY SHARE BAR
                val remainingBudget = maxOf(0f, monthlyBudget - totalSpend)
                val rawPercentage = if (monthlyBudget > 0f) totalSpend / monthlyBudget else 0f
                val clampedPercentage = rawPercentage.coerceIn(0f, 1f)
                val animatedSweep by animateFloatAsState(targetValue = clampedPercentage, animationSpec = tween(1500, delayMillis = 300), label = "donut_sweep")

                val (ringColor, centerLottieRes, statusText) = when {
                    rawPercentage >= 1f -> Triple(Color(0xFFEF4444), R.raw.status_danger, "☠️ Budget Blown")
                    rawPercentage > 0.8f -> Triple(Color(0xFFF59E0B), R.raw.status_warning, "⚠️ Near Limit (>80% used)")
                    else -> Triple(MaterialTheme.colorScheme.secondary, R.raw.status_safe, "🛡️ On Track")
                }

                val centerComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(centerLottieRes))
                val centerProgress by animateLottieCompositionAsState(composition = centerComposition, iterations = LottieConstants.IterateForever)

                val categoryShares = remember(allExpenses) {
                    com.finadapt.adaptivefinance.core.util.CategoryShareCalculator.calculateTopCategories(allExpenses)
                }
                val topSpendItem = categoryShares.maxByOrNull { it.amount }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBudgetSheet = true },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = if (isOverBudget) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = statusText, color = ringColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(remainingBudget, currencySymbol),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 32.sp
                                )
                                Text(
                                    text = "Left this month (${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(totalSpend, currencySymbol)} of ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(monthlyBudget, currencySymbol)} spent)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(color = strokeBgColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 20f, cap = StrokeCap.Round))
                                    drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * animatedSweep, useCenter = false, style = Stroke(width = 20f, cap = StrokeCap.Round))
                                }
                                LottieAnimation(composition = centerComposition, progress = { centerProgress }, modifier = Modifier.size(40.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = strokeBgColor)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Segmented Category-Share Bar & Footer Caption
                        if (allExpenses.isEmpty()) {
                            Text("No spending yet this month", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(strokeBgColor)
                            ) {
                                val colorsList = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.outline
                                )
                                categoryShares.forEachIndexed { index, share ->
                                    Box(
                                        modifier = Modifier
                                            .weight(share.percentage.coerceAtLeast(1f))
                                            .fillMaxHeight()
                                            .background(colorsList[index % colorsList.size])
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategorySheet = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val topCaption = if (topSpendItem != null) {
                                    "Top spend: ${topSpendItem.category}, ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(topSpendItem.amount, currencySymbol)}"
                                } else {
                                    "No top spend recorded"
                                }
                                Text(topCaption, style = MaterialTheme.typography.bodySmall, color = subTextColor)
                                Text("Details >", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // BUDGET DETAIL SHEET
                if (showBudgetSheet) {
                    ModalBottomSheet(onDismissRequest = { showBudgetSheet = false }, containerColor = cardColor) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Budget Overview", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Total Monthly Budget: ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(monthlyBudget, currencySymbol)}", color = subTextColor)
                            Text("Total Spent: ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(totalSpend, currencySymbol)}", color = subTextColor)
                            Text("Remaining: ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(remainingBudget, currencySymbol)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            val daysLeft = 30f // approx
                            val safePerDay = if (daysLeft > 0f) remainingBudget / daysLeft else 0f
                            Text("Safe to spend per day: ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(safePerDay, currencySymbol)}", color = subTextColor)

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showBudgetSheet = false
                                    onNavigateToSettings()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Edit Budget in Settings", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // CATEGORY BREAKDOWN SHEET
                if (showCategorySheet) {
                    ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, containerColor = cardColor) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Category Breakdown", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            categoryShares.forEach { share ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(share.category, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                                    Text("${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(share.amount, currencySymbol)} (${share.percentage.toInt()}%)", color = subTextColor)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    showCategorySheet = false
                                    onNavigateToHistory()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("View in History", color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // 5. RECENT TRANSACTIONS
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onNavigateToHistory) {
                        Text("See all >", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (recentExpenses.isEmpty()) {
                    Text("No expenses yet.", color = subTextColor, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    recentExpenses.forEach { expense ->
                        val hasReceipt = expense.receiptImagePath.isNotEmpty() || expense.items.isNotEmpty()

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedExpenseForDetails = expense },
                            colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(40.dp).background(strokeBgColor, CircleShape), contentAlignment = Alignment.Center) { Text("🏷️", fontSize = 16.sp) }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(expense.merchantName.ifEmpty { expense.category }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasReceipt) { Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Has Receipt", tint = subTextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)) }
                                    Text("- ${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(expense.amount, currencySymbol)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                    }
                }

                // EXTRA PADDING AT BOTTOM TO CLEAR FABS
                Spacer(modifier = Modifier.height(140.dp))
                // EXTRA PADDING AT BOTTOM TO CLEAR FABS
                Spacer(modifier = Modifier.height(140.dp))
            }

            if (levelUpTier != null) {
                LevelUpOverlay(newTierName = levelUpTier, onDismiss = onDismissLevelUp)
            }

            if (pendingAmbushAction != null) {
                GamificationDialog(
                    action = pendingAmbushAction!!,
                    message = pendingAmbushMessage!!,
                    predictionId = pendingAmbushId!!,
                    onFeedback = { predId, rewardInt ->
                        onGameFeedback(predId, pendingAmbushAction!!, rewardInt == 1)
                        prefs.edit { remove("PENDING_AMBUSH_ACTION") }
                        pendingAmbushAction = null
                    },
                    onDismiss = {
                        prefs.edit { remove("PENDING_AMBUSH_ACTION")}
                        pendingAmbushAction = null
                    }
                )
            }

            if (selectedExpenseForDetails != null) {
                val expense = selectedExpenseForDetails!!
                val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

                ModalBottomSheet(onDismissRequest = { selectedExpenseForDetails = null }, sheetState = sheetState, containerColor = cardColor) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(expense.merchantName.ifEmpty { expense.category }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
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
                                            Text(item.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                                            Text(item.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("$currencySymbol ${String.format(Locale.US, "%.2f", item.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                            Text("Total Paid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(expense.amount, currencySymbol), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = gradientStart)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onDeleteExpense(expense)
                                selectedExpenseForDetails = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Transaction", color = MaterialTheme.colorScheme.onError)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedMascotCard(
    userName: String,
    playCoinDrop: Boolean,
    onAnimationFinished: () -> Unit,
    levelName: String,
    tierColorHex: Long,
    fillPercentage: Float,
    xpText: String
) {
    val haptic = LocalHapticFeedback.current
    val tierColor = Color(tierColorHex) 

    val currentLottieRes = when {
        playCoinDrop -> R.raw.piggy_feed
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

    LaunchedEffect(playCoinDrop) {
        if (playCoinDrop) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(piggyProgress) {
        if (playCoinDrop && piggyProgress == 1f) {
            onAnimationFinished()
        }
    }

    val animatedFill by animateFloatAsState(
        targetValue = fillPercentage, 
        animationSpec = tween(1500, delayMillis = 300),
        label = "XP"
    )

    val appTheme = com.finadapt.adaptivefinance.ui.theme.LocalAppTheme.current
    
    var showXpSheet by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { showXpSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = levelName,
                    color = tierColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = xpText,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                        brush = Brush.linearGradient(listOf(appTheme.lightColorScheme.primary, tierColor)),
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

    if (showXpSheet) {
        val nextRankName = when (levelName) {
            "Bronze Novice" -> "Silver Guardian"
            "Silver Guardian" -> "Gold Master"
            "Gold Master" -> "Platinum Legend"
            else -> "Max Rank"
        }

        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showXpSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = levelName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$xpText. Next rank: $nextRankName.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Log an expense", color = MaterialTheme.colorScheme.onBackground)
                    Text("+3 XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stay under daily limit", color = MaterialTheme.colorScheme.onBackground)
                    Text("+20 XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("7-day streak", color = MaterialTheme.colorScheme.onBackground)
                    Text("+50 XP", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showXpSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Got it", color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
