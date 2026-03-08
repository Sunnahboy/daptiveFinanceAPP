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
    onDismissLevelUp: () -> Unit,
    onNavigateToLogExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,





) {
    //FIGMA COLORS
    val gradientStart = Color(0xFF0284C7) // Ocean Blue
    val gradientEnd = Color(0xFF10B981)   // Emerald Green
    val bgColor = Color(0xFFF8FAFC)       // Off-White background

    //FULLY EXPANDED AI CARD LOGIC
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

            // 🟢 1. THE GRADIENT HEADER BACKGROUND
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(gradientStart, gradientEnd)),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            )

            //2. THE SCROLLABLE CONTENT
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
                //--- 2. THE NEW RPG PLAYER PROFILE (Replacing the old XP bar) ---
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFE0F2FE), CircleShape), contentAlignment = Alignment.Center) {
                                Text("RM", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Today's Spend", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            val formattedTodaySpend = String.format(Locale.getDefault(), "%.2f", todaySpend)
                            Text("RM $formattedTodaySpend", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 18.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("This Month", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Text("RM ${totalSpend.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- THE MINI DONUT CHART (Restored!) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Budget Progress", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RM ${totalSpend.toInt()} / RM ${monthlyBudget.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp)
                        }

                        // Donut Logic
                        val rawPercentage = if (monthlyBudget > 0f) totalSpend / monthlyBudget else 0f
                        val clampedPercentage = rawPercentage.coerceIn(0f, 1f)
                        val animatedSweep by animateFloatAsState(targetValue = clampedPercentage * 360f, animationSpec = tween(1500), label = "donut")

                        // Turns red if over budget!
                        val progressColor = if (isOverBudget) Color.Red else Color(0xFF10B981)

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(color = Color(0xFFF1F5F9), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                                drawArc(color = progressColor, startAngle = -90f, sweepAngle = animatedSweep, useCenter = false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                            }
                            if (isOverBudget) {
                                Text("⚠️", fontSize = 18.sp)
                            } else {
                                Text("${(clampedPercentage * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gamification Status", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                            Text("$userXp XP", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(levelName, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                            LinearProgressIndicator(
                                progress = { animatedXpProgress },
                                modifier = Modifier.width(120.dp).height(8.dp),
                                color = Color(0xFFF59E0B),
                                trackColor = Color(0xFFE2E8F0),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- RECENT TRANSACTIONS ---
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(12.dp))

                if (recentExpenses.isEmpty()) {
                    Text("No expenses yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    recentExpenses.forEach { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
                                        Text("🏷️", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(expense.category, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                Text("- RM ${expense.amount}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
            //THE CELEBRATION TRAP
            if (levelUpTier != null) {
                LevelUpOverlay(
                    newTierName = levelUpTier,
                    onDismiss = onDismissLevelUp
                )
            }
        }
    }
}