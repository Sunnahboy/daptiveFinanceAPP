package com.finadapt.adaptivefinance.feature.onboarding

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas // Add this import!
import androidx.compose.ui.geometry.Offset // Add this import!
import androidx.compose.ui.graphics.Brush // Add this import!

//1.CUSTOM COLOR PALETTE
val ThemePrimary = Color(0xFF30E87A)
val ThemeBgLight = Color(0xFFF6F8F7)
val ThemeBgDark = Color(0xFF112117)
val ThemeNavyCustom = Color(0xFF0F1C14)
val ThemeEmeraldAccent = Color(0xFF10B981)

val Slate900 = Color(0xFF0F172A) // Dark text for Light mode
val Slate100 = Color(0xFFF1F5F9) // Light text for Dark mode
val Slate500 = Color(0xFF64748B) // Subtitle gray
val BorderLight = Color(0xFFE2E8F0)
val BorderDark = Color(0x66064E3B) // emerald-900/40

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedGoal by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) ThemeBgDark else ThemeBgLight
    val textColor = if (isDark) Slate100 else Slate900

    // Use Box instead of Surface to layer the background and the UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ✨ THE MAGIC: Ambient Glowing Background (Replicating Tailwind blur-3xl)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top Left Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ThemeEmeraldAccent.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 800f
                )
            )
            // Bottom Right Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ThemePrimary.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width, size.height + 200f),
                    radius = 1000f
                )
            )
        }

        // The Rest of the UI layers on top of the Canvas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), // Added top padding for status bar
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (currentStep == 2) currentStep = 1 }) {
                    if (currentStep == 2) Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text(
                    text = if (currentStep == 1) "Goal Selection" else "Set Baseline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Progress Indicator
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "STEP $currentStep OF 2",
                        color = if (isDark) ThemePrimary else ThemeEmeraldAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentStep == 1) "50% Complete" else "100% Complete",
                        color = Slate500,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (currentStep == 1) 0.5f else 1.0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = ThemePrimary,
                    trackColor = if (isDark) Color(0x4D064E3B) else BorderLight,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }

            // Screen Content
            Crossfade(targetState = currentStep, label = "OnboardingSteps") { step ->
                when (step) {
                    1 -> GoalSelectionStep(
                        selectedGoal = selectedGoal,
                        onGoalSelected = { selectedGoal = it },
                        isDark = isDark,
                        textColor = textColor
                    )
                    2 -> BudgetInputStep(
                        budget = budgetInput,
                        onBudgetChange = { budgetInput = it },
                        isDark = isDark,
                        textColor = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Action Button
            Column(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = {
                        if (currentStep == 1 && selectedGoal.isNotBlank()) {
                            currentStep = 2
                        } else if (currentStep == 2 && budgetInput.isNotBlank()) {
                            coroutineScope.launch {
                                saveUserBaseline(context, selectedGoal, budgetInput)
                                onFinish()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                    shape = RoundedCornerShape(50), // 🟢 CHANGED: Make button a perfect pill
                    enabled = (currentStep == 1 && selectedGoal.isNotBlank()) ||
                            (currentStep == 2 && budgetInput.isNotBlank())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentStep == 1) "Continue to Next Step" else "Complete Setup",
                            color = Color(0xFF022C22),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentStep == 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF022C22))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You can change this later in settings.",
                    color = Slate500,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun GoalSelectionStep(selectedGoal: String, onGoalSelected: (String) -> Unit, isDark: Boolean, textColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "What's your primary financial goal?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select the option that best fits your current financial journey to help the AI personalize your experience.",
            color = Slate500,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Big Interactive Cards
        GoalCard("Save Money",
            "Build your emergency fund or long-term savings.",
             Icons.Default.Star,
            selectedGoal == "Save Money",
             isDark, textColor) { onGoalSelected("Save Money") }
        GoalCard("Stop Overspending",
            "Identify leaks and cut down on unnecessary costs.",
             Icons.Default.ShoppingCart, selectedGoal == "Stop Overspending",
             isDark, textColor) { onGoalSelected("Stop Overspending") }
        GoalCard("Track Daily Habits",
            "Monitor your cashflow and spending daily.",
            Icons.AutoMirrored.Filled.TrendingUp, selectedGoal == "Track Daily Habits", isDark, textColor) { onGoalSelected("Track Daily Habits") }
    }
}

@Composable
fun BudgetInputStep(budget: String, onBudgetChange: (String) -> Unit, isDark: Boolean, textColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Set your monthly baseline.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your estimated monthly budget. The AI Bandit uses this to calculate your spending volatility.",
            color = Slate500,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("Monthly Budget (RM)", color = Slate500) },
            leadingIcon = { Text("RM", modifier = Modifier.padding(start = 16.dp), fontWeight = FontWeight.Bold, color = textColor) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePrimary,
                unfocusedBorderColor = if (isDark) BorderDark else BorderLight,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )
    }
}

@Composable
fun GoalCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    // Make unselected borders invisible in dark mode, light gray in light mode
    val borderColor = if (isSelected) ThemePrimary else if (isDark) Color.Transparent else BorderLight
    val cardBgColor = if (isDark) Color(0x660F1C14) else Color.White
    val iconBgColor = if (isDark) Color(0x33064E3B) else Color(0xFFD1FAE5)
    val iconColor = if (isDark) ThemePrimary else Color(0xFF059669)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp), // 🟢 CHANGED: Sleek pill shape (was 16.dp)
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, RoundedCornerShape(16.dp)), // Adjusted icon box radius
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(text = description, color = Slate500, style = MaterialTheme.typography.bodySmall)
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = ThemePrimary)
            }
        }
    }
}

// Saves the UUID and Baseline
suspend fun saveUserBaseline(context: Context, goal: String, budget: String) {
    withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("SILENT_USER_ID", null)
        prefs.edit {
            if (currentUserId == null) {
                val newUserId = UUID.randomUUID().toString()
                putString("SILENT_USER_ID", newUserId)
                println("Generated new Silent User ID: $newUserId")
            }
            putString("USER_GOAL", goal)
            putFloat("MONTHLY_BUDGET", budget.toFloatOrNull() ?: 0f)
            putString("TEST_GROUP", "adaptive")
        }
    }
    println("Baseline Saved! Goal: $goal, Budget: RM $budget")
}