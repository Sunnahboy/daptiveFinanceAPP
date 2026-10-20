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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID



@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var selectedGoal by rememberSaveable { mutableStateOf("") }
    var currencyInput by rememberSaveable { mutableStateOf("RM") }
    var budgetInput by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    // ================= STRICT VALIDATION LOGIC =================
    val cleanName = nameInput.trim()
    val cleanCurrency = currencyInput.trim()
    val parsedBudget = budgetInput.toFloatOrNull()

    // Name Error States
    val isNameError = nameInput.isNotEmpty() && (cleanName.isEmpty() || cleanName.length > 15)
    val nameErrorMessage = when {
        nameInput.isNotEmpty() && cleanName.isEmpty() -> "Name cannot be empty spaces."
        cleanName.length > 15 -> "Keep it short (max 15 characters)."
        else -> null
    }

    // Currency Error States
    val isCurrencyError = currencyInput.isNotEmpty() && (cleanCurrency.isEmpty() || cleanCurrency.length > 5)
    val currencyErrorMessage = when {
        currencyInput.isNotEmpty() && cleanCurrency.isEmpty() -> "Currency symbol cannot be empty spaces."
        cleanCurrency.length > 5 -> "Keep it short (max 5 characters)."
        else -> null
    }

    // Budget Error States
    val isBudgetError = budgetInput.isNotEmpty() && (parsedBudget == null || parsedBudget <= 0f || parsedBudget > 1_000_000f)
    val budgetErrorMessage = when {
        budgetInput.isNotEmpty() && (parsedBudget == null || parsedBudget <= 0f) -> "Enter a valid amount greater than 0."
        parsedBudget != null && parsedBudget > 1_000_000f -> "Budget exceeds maximum limit."
        else -> null
    }

    // Overall Step Validity
    val isStepValid by remember(currentStep, cleanName, selectedGoal, parsedBudget, cleanCurrency) {
        derivedStateOf {
            when (currentStep) {
                1 -> cleanName.isNotEmpty() && cleanName.length <= 15
                2 -> selectedGoal.isNotBlank()
                3 -> cleanCurrency.isNotEmpty() && cleanCurrency.length <= 5 && parsedBudget != null && parsedBudget > 0f && parsedBudget <= 1_000_000f
                else -> false
            }
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
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                }
                val headerText = when (currentStep) {
                    1 -> "Identity"
                    2 -> "Goal Selection"
                    else -> "Set Baseline"
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            OnboardingProgressBar(currentStep)

            // Step Content
            Crossfade(
                targetState = currentStep,
                modifier = Modifier.weight(1f),
                label = "StepTransition"
            ) { step ->
                when (step) {
                    1 -> NameInputStep(
                        name = nameInput,
                        onNameChange = { newValue ->
                            // SANITIZATION: Actively strip out numbers, dots, commas, and emojis.
                            val sanitized = newValue.filter { it.isLetter() || it.isWhitespace() || it == '-' || it == '\'' }
                            nameInput = sanitized
                        },
                        isError = isNameError,
                        errorMessage = nameErrorMessage,
                        textColor = textColor
                    )
                    2 -> GoalSelectionStep(
                        selectedGoal = selectedGoal,
                        onGoalSelected = {
                            selectedGoal = it
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        textColor = textColor
                    )
                    3 -> BudgetInputStep(
                        currency = currencyInput,
                        onCurrencyChange = { newValue ->
                            val sanitized = newValue.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '$' || it == '€' || it == '£' || it == '¥' }
                            currencyInput = sanitized
                        },
                        isCurrencyError = isCurrencyError,
                        currencyErrorMessage = currencyErrorMessage,
                        budget = budgetInput,
                        onBudgetChange = { newValue ->
                            // SANITIZATION: European commas and filter out bad chars instantly
                            val sanitized = newValue.replace(',', '.')
                            if (sanitized.isEmpty()) {
                                budgetInput = sanitized
                            } else {
                                val filtered = sanitized.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    budgetInput = filtered
                                }
                            }
                        },
                        isError = isBudgetError,
                        errorMessage = budgetErrorMessage,
                        onDone = {
                            if (isStepValid) {
                                coroutineScope.launch {
                                    saveUserBaseline(context, cleanName, selectedGoal, cleanCurrency, budgetInput)
                                    onFinish()
                                }
                            }
                        },
                        textColor = textColor
                    )
                }
            }

            // Footer
            OnboardingFooter(
                currentStep = currentStep,
                isValid = isStepValid,
                onContinue = {
                    if (currentStep < 3) currentStep += 1
                    else {
                        coroutineScope.launch {
                            saveUserBaseline(context, cleanName, selectedGoal, cleanCurrency, budgetInput)
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
    val accentColor = MaterialTheme.colorScheme.secondary
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = 800f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width, size.height + 200f),
                radius = 1000f
            )
        )
    }
}

@Composable
fun OnboardingProgressBar(step: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "STEP $step OF 3",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color(0x4D064E3B),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
    }
}

@Composable
fun NameInputStep(
    name: String,
    onNameChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    textColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Let's get to know you.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("What should your AI guardian call you?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your First Name") },
            singleLine = true,
            isError = isError,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GoalSelectionStep(selectedGoal: String, onGoalSelected: (String) -> Unit, textColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Text("What's your primary goal?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We will tailor your AI gamification dashboard based on your choice.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        ONBOARDING_GOALS.forEach { goal ->
            GoalCard(
                title = goal.title,
                description = goal.desc,
                icon = goal.icon,
                isSelected = selectedGoal == goal.title,
                textColor = textColor,
                onClick = { onGoalSelected(goal.title) }
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val cardBgColor = MaterialTheme.colorScheme.surface
    val iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha=0.15f) // Soft mint green
    val iconColor = MaterialTheme.colorScheme.primary

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
                Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetInputStep(
    currency: String,
    onCurrencyChange: (String) -> Unit,
    isCurrencyError: Boolean,
    currencyErrorMessage: String?,
    budget: String,
    onBudgetChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    onDone: () -> Unit,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val presetCurrencies = listOf("RM", "$", "€", "£", "¥", "₹")

    Column(modifier = Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Text("Set your baseline.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We need a currency and budget to calculate your stats.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currency,
                onValueChange = onCurrencyChange,
                label = { Text("Currency Symbol") },
                singleLine = true,
                isError = isCurrencyError,
                supportingText = {
                    if (currencyErrorMessage != null) {
                        Text(currencyErrorMessage, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorTextColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presetCurrencies.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = {
                            onCurrencyChange(preset)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("Monthly Budget ($currency)") },
            prefix = { Text("$currency ", fontWeight = FontWeight.Bold) },
            singleLine = true,
            isError = isError,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingFooter(currentStep: Int, isValid: Boolean, onContinue: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {

        // DYNAMIC HELPER TEXT: Explains why the button is disabled to prevent user frustration
        if (!isValid) {
            val helperText = when (currentStep) {
                1 -> "Please enter a valid name to continue"
                2 -> "Please select a primary goal to continue"
                3 -> "Please enter your monthly budget to finish"
                else -> ""
            }

            Text(
                text = helperText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = onContinue,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                if (currentStep < 3) "Continue" else "Complete Setup",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            if (currentStep < 3) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

data class GoalData(val title: String, val desc: String, val icon: ImageVector)

private val ONBOARDING_GOALS = listOf(
    GoalData("Save Money", "Secure your future and build an emergency fund.", Icons.Default.Star),
    GoalData("Stop Overspending", "Identify hidden leaks and cut unnecessary costs.", Icons.Default.ShoppingCart),
    GoalData("Track Daily Habits", "See exactly where your money goes every day.", Icons.AutoMirrored.Filled.TrendingUp)
)

suspend fun saveUserBaseline(context: Context, name: String, goal: String, currency: String, budget: String) {
    withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("AdaptiveFinancePrefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("SILENT_USER_ID", null)

        prefs.edit {
            if (currentUserId == null) {
                putString("SILENT_USER_ID", UUID.randomUUID().toString())
            }
            putString("USER_NAME", name)
            putString("USER_GOAL", goal)
            putString("PREFERRED_CURRENCY", currency.ifBlank { "RM" })
            putFloat("MONTHLY_BUDGET", budget.toFloatOrNull() ?: 0f)
            putString("TEST_GROUP", "adaptive")
            putLong("INSTALL_TIMESTAMP", System.currentTimeMillis())
        }
    }
}