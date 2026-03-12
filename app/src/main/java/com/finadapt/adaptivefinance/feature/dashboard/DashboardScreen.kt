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
import com.finadapt.adaptivefinance.ui.components.GamifiedDashboardHeader
import com.finadapt.adaptivefinance.ui.components.LevelUpOverlay
import com.finadapt.adaptivefinance.ui.components.NotificationPermissionHandler
import java.text.SimpleDateFormat
import java.util.*

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
    isDarkMode: Boolean = false, // 🟢 NEW: Dark mode state passed in
    onDismissLevelUp: () -> Unit,
    onNavigateToLogExpense: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Check silently in the background
    NotificationPermissionHandler()

    // 🟢 DYNAMIC THEME COLORS
    val gradientStart = Color(0xFF0284C7) // Ocean Blue stays vibrant
    val gradientEnd = Color(0xFF10B981)   // Emerald Green stays vibrant
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val strokeBgColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9) // For chart backgrounds

    // FULLY EXPANDED AI CARD LOGIC
    val isOverBudget = totalSpend > monthlyBudget

    val (insightColor, insightTitle, insightBody, insightIcon) = when {
        isOverBudget -> listOf(
            Color(0xFFEF4444),
            "🚨 Budget Exceeded",
            "You have spent more than your monthly limit. Please stop spending immediately.",
            Icons.Default.Warning
        )
        currentAiAction == "strict_budget" -> listOf(
            Color(0xFFF59E0B),
            "🔥 Survival Mode",
            "High volatility detected! The AI recommends pausing non-essential spending.",
            Icons.Default.Warning
        )
        currentAiAction == "quiz" -> listOf(
            Color(0xFF8B5CF6),
            "🧠 Financial IQ Test",
            "Review your recent spending habits to earn XP and level up.",
            Icons.Default.Lightbulb
        )
        currentAiAction == "cool_off" -> listOf(
            Color(0xFF06B6D4),
            "❄️ Cool-Off Period",
            "Take a deep breath. A 2-hour 'think-before-you-buy' pause is active.",
            Icons.Default.Schedule
        )
        currentAiAction == "streak_builder" -> listOf(
            Color(0xFF3B82F6),
            "🎯 Streak Builder",
            "Maintain your streak! Consistent, low spending is key.",
            Icons.AutoMirrored.Filled.TrendingUp
        )
        else -> listOf(
            Color(0xFF10B981),
            "✨ Great job!",
            "You're currently within your budget. Keep up the disciplined spending!",
            Icons.Default.AutoAwesome
        )
    }

    val currentDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())

    Scaffold(
        containerColor = bgColor, // 🟢 Applies Dark Mode to background
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLogExpense,
                containerColor = Color(0xFF007AFF), // Apple Blue looks great in both themes
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Expense", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // THE GRADIENT HEADER BACKGROUND (Always dark text on top, so colors remain white)
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

                // THE RPG PLAYER PROFILE
                GamifiedDashboardHeader(
                    totalXp = userXp,
                    currentStreak = currentStreak,
                    userName = userName
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                        colors = CardDefaults.cardColors(containerColor = cardColor), // 🟢 Dynamic
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
                        colors = CardDefaults.cardColors(containerColor = cardColor), // 🟢 Dynamic
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(if (isDarkMode) Color(0xFF10B981).copy(alpha=0.2f) else Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("This Month", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            Text("RM ${totalSpend.toInt()}", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp) // 🟢 Dynamic
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE MINI DONUT CHART ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor), // 🟢 Dynamic
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Budget Progress", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RM ${totalSpend.toInt()} / RM ${monthlyBudget.toInt()}", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp) // 🟢 Dynamic
                        }

                        // Donut Logic
                        val rawPercentage = if (monthlyBudget > 0f) totalSpend / monthlyBudget else 0f
                        val clampedPercentage = rawPercentage.coerceIn(0f, 1f)
                        val animatedSweep by animateFloatAsState(targetValue = clampedPercentage * 360f, animationSpec = tween(1500), label = "donut")
                        val progressColor = if (isOverBudget) Color.Red else Color(0xFF10B981)

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(color = strokeBgColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round)) // 🟢 Dynamic
                                drawArc(color = progressColor, startAngle = -90f, sweepAngle = animatedSweep, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                            }
                            if (isOverBudget) {
                                Text("⚠️", fontSize = 18.sp)
                            } else {
                                Text("${(clampedPercentage * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor) // 🟢 Dynamic
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE SLEEK XP PROGRESS BAR ---
                val levelName = if (userXp >= 500) "Master 👑" else if (userXp >= 200) "Guardian 🛡️" else "Novice 🌱"
                val rawProgress = if (userXp >= 500) 1f else (userXp % 200).toFloat() / 200f
                val animatedXpProgress by animateFloatAsState(targetValue = rawProgress, animationSpec = tween(1000), label = "xpAnim")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor), // 🟢 Dynamic
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gamification Status", color = subTextColor, style = MaterialTheme.typography.labelMedium)
                            Text("$userXp XP", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(levelName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp) // 🟢 Dynamic
                            LinearProgressIndicator(
                                progress = { animatedXpProgress },
                                modifier = Modifier.width(120.dp).height(8.dp),
                                color = Color(0xFFF59E0B),
                                trackColor = strokeBgColor, // 🟢 Dynamic
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- RECENT TRANSACTIONS ---
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor) // 🟢 Dynamic
                Spacer(modifier = Modifier.height(12.dp))

                if (recentExpenses.isEmpty()) {
                    Text("No expenses yet.", color = subTextColor, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    recentExpenses.forEach { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor), // 🟢 Dynamic
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
                                        Text(expense.category, fontWeight = FontWeight.Bold, color = textColor) // 🟢 Dynamic
                                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                }
                                Text("- RM ${expense.amount}", fontWeight = FontWeight.Bold, color = textColor) // 🟢 Dynamic
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // THE CELEBRATION TRAP
            if (levelUpTier != null) {
                LevelUpOverlay(
                    newTierName = levelUpTier,
                    onDismiss = onDismissLevelUp
                )
            }
        }
    }
}