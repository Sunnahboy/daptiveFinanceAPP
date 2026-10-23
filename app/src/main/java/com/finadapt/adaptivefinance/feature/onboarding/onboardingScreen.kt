package com.finadapt.adaptivefinance.feature.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay
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
    var isCompleting by rememberSaveable { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AmbientGlow()

        // Edge-to-edge layout structure: precise insets for top header and bottom footer, eliminating dead space
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        ) {
            // Header with exact status bar padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (currentStep > 1 && !isCompleting) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentStep -= 1
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor, modifier = Modifier.size(20.dp))
                    }
                }
                
                val headerText = when (currentStep) {
                    1 -> "Your Profile"
                    2 -> "Your Vision"
                    else -> "Baseline Setup"
                }
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Step $currentStep of 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sleek Progress Indicator
            OnboardingProgressBar(currentStep)

            // Step Content with Crossfade
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = currentStep,
                    modifier = Modifier.fillMaxSize(),
                    label = "StepTransition"
                ) { step ->
                    when (step) {
                        1 -> NameInputStep(
                            name = nameInput,
                            onNameChange = { newValue ->
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
                                if (isStepValid && !isCompleting) {
                                    isCompleting = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        saveUserBaseline(context, cleanName, selectedGoal, cleanCurrency, budgetInput)
                                        delay(400) // Delightful micro-pause for smooth completion transition
                                        onFinish()
                                    }
                                }
                            },
                            textColor = textColor
                        )
                    }
                }

                // Completion Overlay Animation
                if (isCompleting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Crafting your financial sanctuary...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Footer with exact navigation bar padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                OnboardingFooter(
                    currentStep = currentStep,
                    isValid = isStepValid,
                    isCompleting = isCompleting,
                    onContinue = {
                        if (currentStep < 3) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            currentStep += 1
                        } else if (!isCompleting) {
                            isCompleting = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                saveUserBaseline(context, cleanName, selectedGoal, cleanCurrency, budgetInput)
                                delay(400)
                                onFinish()
                            }
                        }
                    }
                )
            }
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
                colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = 900f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width, size.height + 100f),
                radius = 1100f
            )
        )
    }
}

@Composable
fun OnboardingProgressBar(step: Int) {
    val progressAnim by animateFloatAsState(
        targetValue = step / 3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ProgressBarAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progressAnim },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Welcome to your new financial clarity.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "What shall your AI financial guardian call you?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your First Name") },
            placeholder = { Text("e.g. Alex") },
            singleLine = true,
            isError = isError,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GoalSelectionStep(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "What is your primary focus right now?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "We'll tailor your insights, budgeting style, and AI guardian behavior to match.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        ONBOARDING_GOALS.forEach { goal ->
            GoalCard(
                title = goal.title,
                description = goal.desc,
                icon = goal.icon,
                isSelected = selectedGoal == goal.title,
                textColor = textColor,
                onClick = { onGoalSelected(goal.title) }
            )
            Spacer(modifier = Modifier.height(12.dp))
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
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "GoalCardScale"
    )

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val cardBgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
    val iconBgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconBgColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Establish your financial baseline.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Set your preferred currency and monthly budget target to power smart insights.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(36.dp))

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
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorTextColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presetCurrencies.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onCurrencyChange(preset)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("Monthly Budget Limit") },
            prefix = { Text("$currency ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            placeholder = { Text("3000") },
            singleLine = true,
            isError = isError,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            },
            leadingIcon = {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorTextColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OnboardingFooter(
    currentStep: Int,
    isValid: Boolean,
    isCompleting: Boolean,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Dynamic helper text explaining validity to prevent user friction
        AnimatedVisibility(
            visible = !isValid,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val helperText = when (currentStep) {
                1 -> "Please enter your name to proceed"
                2 -> "Please select a primary goal to proceed"
                3 -> "Please enter a valid monthly budget to finish"
                else -> ""
            }

            Text(
                text = helperText,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = onContinue,
            enabled = isValid && !isCompleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (currentStep < 3) "Continue" else "Complete Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentStep < 3) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class GoalData(val title: String, val desc: String, val icon: ImageVector)

private val ONBOARDING_GOALS = listOf(
    GoalData(
        title = "Build Wealth & Save",
        desc = "Secure your financial future and grow a robust emergency reserve.",
        icon = Icons.Default.Savings
    ),
    GoalData(
        title = "Stop Overspending",
        desc = "Identify financial leaks, curb impulsive purchases, and stay disciplined.",
        icon = Icons.Default.ShoppingCart
    ),
    GoalData(
        title = "Master Daily Habits",
        desc = "Gain crystal-clear visibility into where every dollar flows each day.",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    )
)

suspend fun saveUserBaseline(
    context: Context,
    name: String,
    goal: String,
    currency: String,
    budget: String
) {
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
