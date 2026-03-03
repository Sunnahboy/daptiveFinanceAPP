package com.finadapt.adaptivefinance.feature.onboarding

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// 1. CUSTOM PALETTE
val ThemePrimary = Color(0xFF30E87A)
val ThemeBgLight = Color(0xFFF6F8F7)
val ThemeBgDark = Color(0xFF112117)
val ThemeEmeraldAccent = Color(0xFF10B981)
val Slate900 = Color(0xFF0F172A)
val Slate100 = Color(0xFFF1F5F9)
val Slate500 = Color(0xFF64748B)
val BorderLight = Color(0xFFE2E8F0)
val BorderDark = Color(0x66064E3B)


@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var selectedGoal by rememberSaveable { mutableStateOf("") }
    var budgetInput by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) ThemeBgDark else ThemeBgLight
    val textColor = if (isDark) Slate100 else Slate900

    //Highly efficient validation logic
    val isStepValid by remember(currentStep, selectedGoal, budgetInput) {
        derivedStateOf {
            if (currentStep == 1) selectedGoal.isNotBlank()
            else budgetInput.isNotBlank()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        AmbientGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                if (currentStep == 2) {
                    IconButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                }
                Text(
                    text = if (currentStep == 1) "Goal Selection" else "Set Baseline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            OnboardingProgressBar(currentStep, isDark)

            Crossfade(
                targetState = currentStep,
                modifier = Modifier.weight(1f),
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> GoalSelectionStep(
                        selectedGoal = selectedGoal,
                        onGoalSelected = {
                            selectedGoal = it
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        textColor = textColor,
                        isDark = isDark
                    )
                    2 -> BudgetInputStep(
                        budget = budgetInput,
                        onBudgetChange = { budgetInput = it },
                        onDone = {
                            if (budgetInput.isNotBlank()) {
                                coroutineScope.launch {
                                    saveUserBaseline(context, selectedGoal, budgetInput)
                                    onFinish()
                                }
                            }
                        },
                        textColor = textColor,
                        isDark = isDark
                    )
                }
            }

            //Pass the highly efficient boolean here
            OnboardingFooter(
                currentStep = currentStep,
                isValid = isStepValid,
                onContinue = {
                    if (currentStep == 1) currentStep = 2
                    else {
                        coroutineScope.launch {
                            saveUserBaseline(context, selectedGoal, budgetInput)
                            onFinish()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AmbientGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ThemeEmeraldAccent.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = 800f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ThemePrimary.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width, size.height + 200f),
                radius = 1000f
            )
        )
    }
}

@Composable
fun OnboardingProgressBar(step: Int, isDark: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "STEP $step OF 2",
                color = if (isDark) ThemePrimary else ThemeEmeraldAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (step == 1) "50% Complete" else "100% Complete",
                color = Slate500,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (step == 1) 0.5f else 1.0f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = ThemePrimary,
            trackColor = if (isDark) Color(0x4D064E3B) else BorderLight,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
    }
}

@Composable
fun GoalSelectionStep(selectedGoal: String, onGoalSelected: (String) -> Unit, textColor: Color, isDark: Boolean) {


    Column(modifier = Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Text("What's your primary goal?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We will tailor your AI gamification dashboard based on your choice.", color = Slate500, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        ONBOARDING_GOALS.forEach { goal ->
            GoalCard(
                title = goal.title,
                description = goal.desc,
                icon = goal.icon,
                isSelected = selectedGoal == goal.title,
                isDark = isDark,
                textColor = textColor,
                onClick = { onGoalSelected(goal.title) }
            )
        }
    }
}

//GoalCard back
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
    val borderColor = if (isSelected) ThemePrimary else if (isDark) Color.Transparent else BorderLight
    val cardBgColor = if (isDark) Color(0x660F1C14) else Color.White
    val iconBgColor = if (isDark) Color(0x33064E3B) else Color(0xFFD1FAE5)
    val iconColor = if (isDark) ThemePrimary else Color(0xFF059669)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
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
                    .background(iconBgColor, RoundedCornerShape(16.dp)),
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

@Composable
fun BudgetInputStep(budget: String, onBudgetChange: (String) -> Unit, onDone: () -> Unit, textColor: Color, isDark: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Set your baseline.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("The AI uses this to calculate volatility.", color = Slate500, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("Monthly Budget (RM)") },
            prefix = { Text("RM ", fontWeight = FontWeight.Bold) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePrimary,
                unfocusedBorderColor = if (isDark) BorderDark else BorderLight,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingFooter(currentStep: Int, isValid: Boolean, onContinue: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Button(
            onClick = onContinue,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                if (currentStep == 1) "Continue" else "Complete Setup",
                color = Color(0xFF022C22),
                fontWeight = FontWeight.Bold
            )
            if (currentStep == 1) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF022C22))
            }
        }
    }
}

// 4. DATA MODELS & UTILS
data class GoalData(val title: String, val desc: String, val icon: ImageVector)

//created exactly ONCE in memory.
private val ONBOARDING_GOALS = listOf(
    GoalData("Save Money", "Secure your future and build an emergency fund.", Icons.Default.Star),
    GoalData("Stop Overspending", "Identify hidden leaks and cut unnecessary costs.", Icons.Default.ShoppingCart),
    GoalData("Track Daily Habits", "See exactly where your money goes every day.", Icons.AutoMirrored.Filled.TrendingUp)
)

suspend fun saveUserBaseline(context: Context, goal: String, budget: String) {
    withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        // FIX: Read from prefs directly before opening the edit block
        val currentUserId = prefs.getString("SILENT_USER_ID", null)

        prefs.edit {
            if (currentUserId == null) {
                putString("SILENT_USER_ID", UUID.randomUUID().toString())
            }
            putString("USER_GOAL", goal)
            putFloat("MONTHLY_BUDGET", budget.toFloatOrNull() ?: 0f)
            // the AI Gamified group for testing
            putString("TEST_GROUP", "adaptive")
            //Save the exact moment they started the app
            putLong("INSTALL_TIMESTAMP", System.currentTimeMillis())
        }
    }
}