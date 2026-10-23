
package com.finadapt.adaptivefinance.feature.dashboard

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf

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
    onGameFeedback: (String, String, Boolean) -> Unit,
    onNavigateToTab: (Int, String?) -> Unit = { _, _ -> }
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
    var isSandboxReplay by remember { mutableStateOf(false) }

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
                                    modifier = Modifier.size(24.dp).offset(x = (-2).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatDays(currentStreak),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (showStreakSheet) {
                        var viewedMonthCal by remember { mutableStateOf(Calendar.getInstance()) }
                        var selectedDayCal by remember { mutableStateOf<Calendar?>(null) }
                        val haptics = LocalHapticFeedback.current

                        val expenseMap = remember(allExpenses) {
                            allExpenses.groupBy { exp ->
                                val cal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
                            }
                        }

                        ModalBottomSheet(
                            onDismissRequest = { showStreakSheet = false },
                            containerColor = cardColor
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                contentPadding = PaddingValues(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔥", fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatDays(currentStreak) + " Streak",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "Mindful daily financial journal",
                                                    color = subTextColor,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewedMonthCal = Calendar.getInstance()
                                                selectedDayCal = Calendar.getInstance()
                                            },
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                                Text(
                                                    text = monthFormat.format(viewedMonthCal.time),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewedMonthCal = (viewedMonthCal.clone() as Calendar).apply {
                                                            add(Calendar.MONTH, -1)
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.onBackground)
                                                    }
                                                    IconButton(onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewedMonthCal = (viewedMonthCal.clone() as Calendar).apply {
                                                            add(Calendar.MONTH, 1)
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onBackground)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayStr ->
                                                    Text(
                                                        text = dayStr,
                                                        color = subTextColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(36.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val daysInMonth = (viewedMonthCal.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                            }.get(Calendar.DAY_OF_MONTH)

                                            val firstDayOfWeek = (viewedMonthCal.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, 1)
                                            }.get(Calendar.DAY_OF_WEEK)
                                            val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

                                            val totalCells = offset + daysInMonth
                                            val rows = (totalCells + 6) / 7
                                            val todayCal = Calendar.getInstance()

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                for (row in 0 until rows) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                        for (col in 0 until 7) {
                                                            val cellIndex = row * 7 + col
                                                            val dayNum = cellIndex - offset + 1

                                                            if (dayNum in 1..daysInMonth) {
                                                                val cellCal = (viewedMonthCal.clone() as Calendar).apply {
                                                                    set(Calendar.DAY_OF_MONTH, dayNum)
                                                                }
                                                                val dateKey = "${cellCal.get(Calendar.YEAR)}-${cellCal.get(Calendar.MONTH)}-${cellCal.get(Calendar.DAY_OF_MONTH)}"
                                                                val dayExpenses = expenseMap[dateKey] ?: emptyList()
                                                                val dayTotal = dayExpenses.sumOf { it.amount.toDouble() }.toFloat()

                                                                val isToday = cellCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                                                        cellCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                                                                val isSelected = selectedDayCal != null &&
                                                                        cellCal.get(Calendar.YEAR) == selectedDayCal!!.get(Calendar.YEAR) &&
                                                                        cellCal.get(Calendar.DAY_OF_YEAR) == selectedDayCal!!.get(Calendar.DAY_OF_YEAR)

                                                                val heatColor = when {
                                                                    dayTotal > 100f -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                                                    dayTotal > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                                    else -> MaterialTheme.colorScheme.surface
                                                                }

                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(40.dp)
                                                                        .clip(RoundedCornerShape(12.dp))
                                                                        .background(
                                                                            when {
                                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                                isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                                                                else -> heatColor
                                                                            }
                                                                        )
                                                                        .border(
                                                                            width = if (isToday) 1.5.dp else 0.dp,
                                                                            color = if (isToday) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                                                            shape = RoundedCornerShape(12.dp)
                                                                        )
                                                                        .clickable {
                                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            selectedDayCal = cellCal
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        verticalArrangement = Arrangement.Center
                                                                    ) {
                                                                        Text(
                                                                            text = dayNum.toString(),
                                                                            fontSize = 13.sp,
                                                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                                                                        )
                                                                        if (dayTotal > 0f) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(4.dp)
                                                                                    .background(
                                                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                                                        CircleShape
                                                                                    )
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                Spacer(modifier = Modifier.size(40.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    val activeSelectedCal = selectedDayCal ?: Calendar.getInstance()
                                    val selectedDateKey = "${activeSelectedCal.get(Calendar.YEAR)}-${activeSelectedCal.get(Calendar.MONTH)}-${activeSelectedCal.get(Calendar.DAY_OF_MONTH)}"
                                    val selectedDayExpenses = expenseMap[selectedDateKey] ?: emptyList()
                                    val selectedDayTotal = selectedDayExpenses.sumOf { it.amount.toDouble() }.toFloat()
                                    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = dateFormat.format(activeSelectedCal.time),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (selectedDayExpenses.isNotEmpty()) "${selectedDayExpenses.size} transactions logged" else "Zero spend day",
                                                        fontSize = 12.sp,
                                                        color = subTextColor
                                                    )
                                                }
                                                Text(
                                                    text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(selectedDayTotal, currencySymbol),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (selectedDayTotal > 0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (selectedDayExpenses.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    selectedDayExpenses.map { it.category }.distinct().take(3).forEach { cat ->
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.surface,
                                                            shape = RoundedCornerShape(50)
                                                        ) {
                                                            Text(
                                                                text = cat,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            } else {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "No expenses recorded on this day. Keep that streak protected! ✨",
                                                    fontSize = 12.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        }
                                    }
                                }

                                val activeSelectedCalForList = selectedDayCal ?: Calendar.getInstance()
                                val selectedDateKeyForList = "${activeSelectedCalForList.get(Calendar.YEAR)}-${activeSelectedCalForList.get(Calendar.MONTH)}-${activeSelectedCalForList.get(Calendar.DAY_OF_MONTH)}"
                                val selectedDayExpensesForList = expenseMap[selectedDateKeyForList] ?: emptyList()

                                items(selectedDayExpensesForList) { exp ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("💳", fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = exp.merchantName.ifEmpty { exp.category },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = exp.category,
                                                        fontSize = 11.sp,
                                                        color = subTextColor
                                                    )
                                                }
                                            }
                                            Text(
                                                text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(exp.amount, currencySymbol),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showStreakSheet = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Done", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
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

            // 1. DYNAMIC ACTIONABLE TIP CARD (Interactive with bulb 💡)
            data class ActionableTip(val message: String, val targetTab: Int, val highlightKey: String?)
            val tips = listOf(
                ActionableTip(dynamicSubtitle, 1, null), // History
                ActionableTip("[Settings] Secure your financial records — Tap to export your financial report.", 6, "export"), // Settings
                ActionableTip("[Settings] Personalize your experience — Tap to customize theme & colors.", 6, "theme"), // Settings
                ActionableTip("[Community] See how others are saving — Tap to view the community leaderboard.", 3, null), // Community
                ActionableTip("[Rewards] Climb the Mountain of Prosperity — Tap to check your rewards & ascent.", 4, null) // Rewards
            )
            val currentTipIndex = remember(allExpenses, dynamicSubtitle) {
                ((System.currentTimeMillis() / 20000) % tips.size).toInt()
            }
            val activeTip = tips[currentTipIndex]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab(activeTip.targetTab, activeTip.highlightKey) },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = activeTip.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SHEET STATES FOR INTERACTIVITY
            var showBudgetSheet by remember { mutableStateOf(false) }
            var showCategorySheet by remember { mutableStateOf(false) }

            // 2. ON-TRACK BUDGET CARD (Hero Card with Ring on Left & Iterative Pie Chart Visual)
            val remainingBudget = maxOf(0f, monthlyBudget - totalSpend)
            val rawPercentage = if (monthlyBudget > 0f) totalSpend / monthlyBudget else 0f
            val clampedPercentage = rawPercentage.coerceIn(0f, 1f)
            val animatedSweep by animateFloatAsState(targetValue = clampedPercentage, animationSpec = tween(1500, delayMillis = 300), label = "donut_sweep")

            val statusInfo = when {
                rawPercentage >= 1f -> listOf<Any>(Color(0xFFEF4444), R.raw.status_danger, "Budget exceeded", Color(0xFFEF4444).copy(alpha = 0.12f))
                rawPercentage > 0.8f -> listOf<Any>(Color(0xFFF59E0B), R.raw.status_warning, "Nearing limit", Color(0xFFF59E0B).copy(alpha = 0.12f))
                else -> listOf<Any>(MaterialTheme.colorScheme.secondary, R.raw.status_safe, "On track", MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            }
            val ringColor = statusInfo[0] as Color
            val centerLottieRes = statusInfo[1] as Int
            val statusText = statusInfo[2] as String
            val statusContainerColor = statusInfo[3] as Color

            val centerComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(centerLottieRes))
            val centerProgress by animateLottieCompositionAsState(composition = centerComposition, iterations = LottieConstants.IterateForever)

            val categoryShares = remember(allExpenses) {
                com.finadapt.adaptivefinance.core.util.CategoryShareCalculator.calculateTopCategories(allExpenses)
            }
            val topSpendItem = categoryShares.maxByOrNull { it.amount }

            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            val colorsList = listOf(primaryColor, secondaryColor, tertiaryColor, outlineColor)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBudgetSheet = true },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = if (isOverBudget) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ring on the Left
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(color = strokeBgColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                                drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * animatedSweep, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                            }
                            LottieAnimation(composition = centerComposition, progress = { centerProgress }, modifier = Modifier.size(38.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Budget Info on the Right
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Surface(
                                color = statusContainerColor,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(ringColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = statusText,
                                        color = ringColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(remainingBudget, currencySymbol),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Left (${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(totalSpend, currencySymbol)} spent)",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = strokeBgColor.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Iterative Pie Chart & Category Breakdown Visual
                    if (allExpenses.isEmpty()) {
                        Text("No spending recorded yet this month", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Mini Iterative Pie Chart
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = -90f
                                    categoryShares.forEachIndexed { index, share ->
                                        val sweepAngle = 360f * (share.percentage / 100f)
                                        drawArc(
                                            color = colorsList[index % colorsList.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle.coerceAtLeast(3f),
                                            useCenter = true,
                                            style = Stroke(width = 0f)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(cardColor, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Category Breakdown Pills
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                categoryShares.take(2).forEachIndexed { index, share ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(colorsList[index % colorsList.size], CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = share.category,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${share.percentage.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = subTextColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategorySheet = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val topCaption = if (topSpendItem != null) {
                                "Top: ${topSpendItem.category} (${com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(topSpendItem.amount, currencySymbol)})"
                            } else {
                                "No top spend recorded"
                            }
                            Text(topCaption, style = MaterialTheme.typography.bodySmall, color = subTextColor, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Category Details", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("›", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // BUDGET DETAIL SHEET (Vico Line Graph & Spending Pace with Iterative Time Horizons)
            if (showBudgetSheet) {
                var selectedHorizon by remember { mutableStateOf(7) }

                ModalBottomSheet(onDismissRequest = { showBudgetSheet = false }, containerColor = cardColor) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Monthly Spending Pace", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Text("Your spending velocity over time", color = subTextColor, fontSize = 13.sp)
                            }
                        }

                        // Iterative Horizon Selector Chips (7D, 14D, 30D)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7, 14, 30).forEach { horizon ->
                                val isSelected = selectedHorizon == horizon
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedHorizon = horizon },
                                    label = { Text("$horizon Days", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Cartesian Line Chart (Vico) with dynamic horizon
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        val todayMidnight = cal.timeInMillis
                        val totals = FloatArray(selectedHorizon)
                        val labels = mutableListOf<String>()
                        val format = SimpleDateFormat(if (selectedHorizon == 30) "MMM d" else "EEE", Locale.getDefault())

                        for (i in (selectedHorizon - 1) downTo 0) {
                            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            labels.add(format.format(c.time))
                        }
                        allExpenses.forEach { exp ->
                            val daysAgo = ((todayMidnight - exp.timestamp) / (1000 * 60 * 60 * 24)).toInt()
                            val idx = if (exp.timestamp >= todayMidnight) selectedHorizon - 1 else (selectedHorizon - 1) - (daysAgo + 1)
                            if (idx in 0 until selectedHorizon) totals[idx] += exp.amount
                        }
                        if (totals.sum() == 0f) totals[0] = 0.01f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Chart(
                                    chart = lineChart(
                                        lines = listOf(
                                            com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec(
                                                lineColor = MaterialTheme.colorScheme.secondary.toArgb()
                                            )
                                        )
                                    ),
                                    model = entryModelOf(*totals.toTypedArray()),
                                    startAxis = rememberStartAxis(
                                        label = textComponent(color = subTextColor, textSize = 10.sp),
                                        valueFormatter = AxisValueFormatter { value, _ -> "$currencySymbol ${value.toInt()}" }
                                    ),
                                    bottomAxis = rememberBottomAxis(
                                        label = textComponent(color = subTextColor, textSize = 10.sp),
                                        valueFormatter = AxisValueFormatter { value, _ -> labels.getOrNull(value.toInt()) ?: "" },
                                        guideline = null
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(150.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Monthly Budget", color = subTextColor)
                            Text(com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(monthlyBudget, currencySymbol), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Spent", color = subTextColor)
                            Text(com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(totalSpend, currencySymbol), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining", color = subTextColor)
                            Text(com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(remainingBudget, currencySymbol), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        val daysLeft = 30f // approx
                        val safePerDay = if (daysLeft > 0f) remainingBudget / daysLeft else 0f
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Safe to spend per day", color = subTextColor)
                            Text(com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(safePerDay, currencySymbol), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showBudgetSheet = false
                                onNavigateToSettings()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Edit Budget in Settings", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. UNIFIED MASCOT CARD
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

            // 4. THE AI ACTION / CHALLENGE BANNER (Persistent & Well-Crafted)
            // 4. THE AI ACTION / CHALLENGE BANNER (Persistent & Well-Crafted with Replay Prompt)
            if (currentAiAction != "zen") {
                val (actionColor, actionTitle, actionDesc) = when (currentAiAction) {
                    "strict_budget" -> Triple(Color(0xFFF59E0B), "🔥 Survival Mode Active", "High volatility detected! The AI recommends pausing non-essential spending.")
                    "quiz" -> Triple(Color(0xFF8B5CF6), "🧠 Financial IQ Test", "Review your recent spending habits to earn XP and level up.")
                    "cool_off" -> Triple(Color(0xFF06B6D4), "❄️ Cool-Off Period Active", "A 2-hour thinking pause is active to protect your budget.")
                    "streak_builder" -> Triple(Color(0xFF3B82F6), "🎯 Streak Builder", "Maintain your daily streak! Consistent, low spending is key.")
                    else -> Triple(MaterialTheme.colorScheme.primary, "🤖 AI Challenge", "Active financial guidance to keep you on track.")
                }

                var showReplayPromptSheet by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReplayPromptSheet = true },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, actionColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(22.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(actionColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (currentAiAction) {
                                        "strict_budget" -> "🔥"
                                        "quiz" -> "🧠"
                                        "cool_off" -> "❄️"
                                        "streak_builder" -> "🎯"
                                        else -> "🤖"
                                    },
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = actionColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Active Challenge",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        color = actionColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = actionTitle,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = actionDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = actionColor)
                    }
                }

                if (showReplayPromptSheet) {
                    ModalBottomSheet(onDismissRequest = { showReplayPromptSheet = false }, containerColor = cardColor) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Replay Challenge", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Would you like to replay this challenge? (Sandboxed Practice Mode — safe practice that does not affect your reinforcement learning model).", color = subTextColor, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    showReplayPromptSheet = false
                                    isSandboxReplay = true
                                    pendingAmbushAction = currentAiAction
                                    pendingAmbushMessage = actionDesc
                                    pendingAmbushId = "sandbox_replay_${System.currentTimeMillis()}"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = actionColor)
                            ) {
                                Text("Play (Sandboxed Replay)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    prefs.edit { putString("LAST_AI_ACTION", "zen") }
                                    showReplayPromptSheet = false
                                    dashboardViewModel.loadDashboardData()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss Challenge", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

                Spacer(modifier = Modifier.height(24.dp))

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
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
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
                    isSandbox = isSandboxReplay,
                    onFeedback = { predId, rewardInt ->
                        if (!isSandboxReplay) {
                            onGameFeedback(predId, pendingAmbushAction!!, rewardInt == 1)
                        }
                        prefs.edit { remove("PENDING_AMBUSH_ACTION") }
                        pendingAmbushAction = null
                        isSandboxReplay = false
                    },
                    onDismiss = {
                        prefs.edit { remove("PENDING_AMBUSH_ACTION")}
                        pendingAmbushAction = null
                        isSandboxReplay = false
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
