package com.finadapt.adaptivefinance.feature.expense.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.finadapt.adaptivefinance.R
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.finadapt.adaptivefinance.feature.export.ReportGenerator
import com.finadapt.adaptivefinance.worker.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentBudget: Float,
    appearanceMode: String,
    currentThemeStr: String,
    currentCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    allExpenses: List<ExpenseEntity>,
    onNameChanged: (String) -> Unit,
    onBudgetChanged: (Float) -> Unit,
    onAppearanceModeChanged: (String) -> Unit,
    onAppThemeChanged: (String) -> Unit,
    onResetGamification: () -> Unit,
    onWipeData: () -> Unit,
    highlightSection: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val profileRequester = remember { BringIntoViewRequester() }
    val preferencesRequester = remember { BringIntoViewRequester() }
    val exportRequester = remember { BringIntoViewRequester() }

    var highlightedCard by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(highlightSection) {
        if (!highlightSection.isNullOrEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            highlightedCard = highlightSection
            try {
                when (highlightSection) {
                    "profile" -> profileRequester.bringIntoView()
                    "theme", "notifications" -> preferencesRequester.bringIntoView()
                    "export" -> exportRequester.bringIntoView()
                }
            } catch (_: Exception) {}

            delay(2500)
            highlightedCard = null
        }
    }
    var nameInput by remember { mutableStateOf(currentName) }
    var currencyInput by remember { mutableStateOf(currentCurrency) }

    val initialBudgetString = if (currentBudget % 1 == 0f) currentBudget.toInt().toString() else currentBudget.toString()
    var budgetInput by remember { mutableStateOf(initialBudgetString) }

    var reminderTimes by remember { mutableStateOf(NotificationScheduler.getTimesFromPrefs(context)) }

    // INDIVIDUAL BUTTON STATES
    var nameActionState by remember { mutableStateOf("IDLE") }
    var currencyActionState by remember { mutableStateOf("IDLE") }
    var budgetActionState by remember { mutableStateOf("IDLE") }

    // EXPORT STATES
    var isGenerating by remember { mutableStateOf(false) }
    var reportType by remember { mutableStateOf(ReportGenerator.Timeframe.MONTHLY) }

    val currentCal = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedWeek by remember { mutableIntStateOf(currentCal.get(Calendar.WEEK_OF_MONTH)) }
    var selectedDay by remember { mutableIntStateOf(currentCal.get(Calendar.DAY_OF_MONTH)) }

    var monthExpanded by remember { mutableStateOf(false) }
    var weekExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val monthsList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val weeksList = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5")

    // DIALOG STATES
    var showValidationErrorDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val appTheme = com.finadapt.adaptivefinance.ui.theme.LocalAppTheme.current
    
    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.colorScheme.secondary
    val infoColor = Color(0xFF64748B)
    val errorColor = MaterialTheme.colorScheme.error
    val outlineBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    // ================= REAL-TIME VALIDATION LOGIC =================

    val cleanName = nameInput.trim()
    val isNameError = cleanName.isEmpty() || cleanName.length > 15
    val nameErrorMessage = when {
        cleanName.isEmpty() -> "Name cannot be empty."
        cleanName.length > 15 -> "Keep it short (max 15 characters)."
        else -> null
    }

    val cleanCurrency = currencyInput.trim()
    val isCurrencyError = cleanCurrency.isEmpty() || cleanCurrency.length > 5
    val currencyErrorMessage = when {
        cleanCurrency.isEmpty() -> "Currency symbol cannot be empty."
        cleanCurrency.length > 5 -> "Keep it short (max 5 characters)."
        else -> null
    }

    val parsedBudget = budgetInput.toFloatOrNull()
    val isBudgetError = budgetInput.isNotEmpty() && (parsedBudget == null || parsedBudget <= 0f || parsedBudget > 1_000_000f)
    val budgetErrorMessage = when {
        budgetInput.isEmpty() -> "Budget cannot be empty."
        parsedBudget == null || parsedBudget <= 0f -> "Enter a valid amount greater than 0."
        parsedBudget > 1_000_000f -> "Budget exceeds maximum limit."
        else -> null
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    scrolledContainerColor = bgColor
                ),
                windowInsets = WindowInsets(0), // Removed default TopAppBar insets to allow it to sit perfectly flush against the system status bar
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 80.dp), // Set top padding to 0.dp
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ─────────────────────────────────────────
            // 1. ACCOUNT & PROFILE (Modular Cards)
            // ─────────────────────────────────────────
            SettingsSectionHeader("Account & Profile", textColor)

            // Card 1: Preferred Name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(profileRequester),
                colors = CardDefaults.cardColors(
                    containerColor = if (highlightedCard == "profile") primaryColor.copy(alpha = 0.15f) else cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = if (highlightedCard == "profile") androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { newValue ->
                            val sanitized = newValue.filter {
                                it.isLetter() || it.isWhitespace() || it == '-' || it == '\''
                            }
                            nameInput = sanitized
                            nameActionState = "IDLE"
                        },
                        label = { Text("Preferred Name") },
                        singleLine = true,
                        isError = isNameError,
                        supportingText = {
                            if (nameErrorMessage != null) Text(nameErrorMessage, color = errorColor)
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (cleanName == currentName) {
                                coroutineScope.launch { nameActionState = "NO_CHANGE"; delay(2000); nameActionState = "IDLE" }
                            } else {
                                onNameChanged(cleanName)
                                coroutineScope.launch { nameActionState = "SUCCESS"; delay(2000); nameActionState = "IDLE" }
                            }
                        },
                        enabled = !isNameError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = when (nameActionState) { "SUCCESS" -> successColor; "NO_CHANGE" -> infoColor; else -> primaryColor })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (nameActionState) {
                                "SUCCESS" -> { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Saved", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                "NO_CHANGE" -> { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Already set", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                else -> Text("Save Name", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Card 2: Preferred Currency Symbol
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { newValue ->
                            val sanitized = newValue.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '$' || it == '€' || it == '£' || it == '¥' }
                            currencyInput = sanitized
                            currencyActionState = "IDLE"
                        },
                        label = { Text("Preferred Currency Symbol") },
                        singleLine = true,
                        isError = isCurrencyError,
                        supportingText = {
                            if (currencyErrorMessage != null) Text(currencyErrorMessage, color = errorColor)
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (cleanCurrency == currentCurrency) {
                                coroutineScope.launch { currencyActionState = "NO_CHANGE"; delay(2000); currencyActionState = "IDLE" }
                            } else {
                                onCurrencyChanged(cleanCurrency)
                                coroutineScope.launch { currencyActionState = "SUCCESS"; delay(2000); currencyActionState = "IDLE" }
                            }
                        },
                        enabled = !isCurrencyError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = when (currencyActionState) { "SUCCESS" -> successColor; "NO_CHANGE" -> infoColor; else -> primaryColor })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (currencyActionState) {
                                "SUCCESS" -> { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Saved", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                "NO_CHANGE" -> { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Already set", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                else -> Text("Save Currency", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Card 3: Monthly Budget Baseline
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text("Monthly Budget Baseline", fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Used by the AI to calculate spending volatility.", color = subTextColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { newValue ->
                            val sanitized = newValue.replace(',', '.')
                            if (sanitized.isEmpty()) {
                                budgetInput = sanitized
                            } else {
                                val filtered = sanitized.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) {
                                    budgetInput = filtered
                                }
                            }
                            budgetActionState = "IDLE"
                        },
                        label = { Text("Budget Limit ($currencyInput)") },
                        prefix = { Text("$currencyInput ", color = textColor) },
                        singleLine = true,
                        isError = isBudgetError || budgetInput.isEmpty(),
                        supportingText = {
                            if (budgetErrorMessage != null) Text(budgetErrorMessage, color = errorColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (parsedBudget != null && parsedBudget == currentBudget) {
                                coroutineScope.launch { budgetActionState = "NO_CHANGE"; delay(2000); budgetActionState = "IDLE" }
                            } else if (parsedBudget != null) {
                                onBudgetChanged(parsedBudget)
                                coroutineScope.launch { budgetActionState = "SUCCESS"; delay(2000); budgetActionState = "IDLE" }
                            }
                        },
                        enabled = !isBudgetError && budgetInput.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = when (budgetActionState) { "SUCCESS" -> successColor; "NO_CHANGE" -> infoColor; else -> primaryColor })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (budgetActionState) {
                                "SUCCESS" -> { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Updated", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                "NO_CHANGE" -> { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("No change", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                else -> Text("Update Budget", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────
            // 2. PREFERENCES
            // ─────────────────────────────────────────
            SettingsSectionHeader("Preferences", textColor)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(preferencesRequester),
                colors = CardDefaults.cardColors(
                    containerColor = if (highlightedCard == "theme" || highlightedCard == "notifications") primaryColor.copy(alpha = 0.15f) else cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = if (highlightedCard == "theme" || highlightedCard == "notifications") androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column {
                    // APPEARANCE PICKER
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("Appearance", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Choose how the app looks.", color = subTextColor, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                                FilterChip(
                                    selected = appearanceMode == mode,
                                    onClick = { onAppearanceModeChanged(mode) },
                                    label = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = cardColor,
                                        labelColor = textColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f))
                    
                    // NEW THEME PICKER UI
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("App Color Theme", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Personalize your Adaptive Finance experience.", color = subTextColor, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            com.finadapt.adaptivefinance.ui.theme.AppTheme.entries.forEach { theme ->
                                val isSelected = currentThemeStr == theme.name
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(theme.lightColorScheme.primary, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) textColor else subTextColor.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { onAppThemeChanged(theme.name) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f))

                    // THE NOTIFICATION TIMES UI
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Reminders", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("When should the AI remind you?", color = subTextColor, fontSize = 12.sp)
                            }
                            if (reminderTimes.size < 3) {
                                IconButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val newTimes = reminderTimes.toMutableList()
                                                newTimes.add(Pair(hourOfDay, minute))
                                                reminderTimes = newTimes

                                                NotificationScheduler.saveTimesToPrefs(context, newTimes)
                                                NotificationScheduler.scheduleDailyReminders(context, newTimes)
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                                        .size(38.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Time", tint = primaryColor)
                                }
                            }
                        }

                        if (reminderTimes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        reminderTimes.forEachIndexed { index, time ->
                            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, time.first); set(Calendar.MINUTE, time.second) }
                            val timeFormatted = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)

                            Surface(
                                color = bgColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(timeFormatted, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            val newTimes = reminderTimes.toMutableList()
                                            newTimes.removeAt(index)
                                            reminderTimes = newTimes

                                            NotificationScheduler.saveTimesToPrefs(context, newTimes)
                                            NotificationScheduler.scheduleDailyReminders(context, newTimes)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = errorColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f))

                    SettingsRowAction(
                        icon = Icons.Default.Notifications, title = "Push Notifications", subtitle = "Manage system alerts",
                        textColor = textColor, subTextColor = subTextColor,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // ─────────────────────────────────────────
            // 3. DATA & EXPORT
            // ─────────────────────────────────────────
            SettingsSectionHeader("Data & Export", textColor)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(exportRequester),
                colors = CardDefaults.cardColors(
                    containerColor = if (highlightedCard == "export") primaryColor.copy(alpha = 0.15f) else cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = if (highlightedCard == "export") androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Text("Select Timeframe:", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportGenerator.Timeframe.entries.forEach { timeframe ->
                            FilterChip(
                                selected = reportType == timeframe,
                                onClick = { reportType = timeframe },
                                label = { Text(timeframe.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = cardColor,
                                    labelColor = textColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // MONTH DROPDOWN
                    ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = !monthExpanded }) {
                        OutlinedTextField(
                            value = "${monthsList[selectedMonth]} $selectedYear",
                            onValueChange = {}, readOnly = true, label = { Text("Select Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor, unfocusedTextColor = textColor,
                                focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                                focusedLabelColor = primaryColor, unfocusedLabelColor = subTextColor
                            )
                        )
                        ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }, containerColor = cardColor) {
                            monthsList.forEachIndexed { index, monthName ->
                                DropdownMenuItem(
                                    text = { Text("$monthName $selectedYear", color = textColor) },
                                    onClick = { selectedMonth = index; monthExpanded = false }
                                )
                            }
                        }
                    }

                    // WEEK DROPDOWN
                    if (reportType == ReportGenerator.Timeframe.WEEKLY) {
                        ExposedDropdownMenuBox(expanded = weekExpanded, onExpandedChange = { weekExpanded = !weekExpanded }) {
                            OutlinedTextField(
                                value = "Week $selectedWeek",
                                onValueChange = {}, readOnly = true, label = { Text("Select Week") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                                    focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                                    focusedLabelColor = primaryColor, unfocusedLabelColor = subTextColor
                                )
                            )
                            ExposedDropdownMenu(expanded = weekExpanded, onDismissRequest = { weekExpanded = false }, containerColor = cardColor) {
                                weeksList.forEachIndexed { index, weekName ->
                                    DropdownMenuItem(
                                        text = { Text(weekName, color = textColor) },
                                        onClick = { selectedWeek = index + 1; weekExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // DAY DROPDOWN
                    if (reportType == ReportGenerator.Timeframe.DAILY) {
                        ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = !dayExpanded }) {
                            OutlinedTextField(
                                value = "Day $selectedDay",
                                onValueChange = {}, readOnly = true, label = { Text("Select Day") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                                    focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.4f),
                                    focusedLabelColor = primaryColor, unfocusedLabelColor = subTextColor
                                )
                            )
                            ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }, containerColor = cardColor) {
                                (1..31).forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text("Day $day", color = textColor) },
                                        onClick = { selectedDay = day; dayExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // EXCEL EXPORT
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                val targetCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    if (reportType == ReportGenerator.Timeframe.WEEKLY) set(Calendar.WEEK_OF_MONTH, selectedWeek)
                                    if (reportType == ReportGenerator.Timeframe.DAILY) set(Calendar.DAY_OF_MONTH, selectedDay)
                                }

                                val filteredData = filterExpensesByCustomDate(allExpenses, reportType, selectedMonth, selectedYear, selectedWeek, selectedDay)

                                if (filteredData.isEmpty()) {
                                    isGenerating = false
                                    android.widget.Toast.makeText(context, "No expenses logged for this period!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val periodLabel = getFormattedPeriodLabel(reportType, targetCal.timeInMillis)
                                val file = withContext(Dispatchers.IO) {
                                    ReportGenerator.generateExcelReport(context, filteredData, periodLabel, currentCurrency)
                                }

                                isGenerating = false
                                if (file != null) ReportGenerator.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = successColor)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Export as Excel (.xlsx)", fontWeight = FontWeight.Bold)
                    }

                    // PDF EXPORT
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                val targetCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    if (reportType == ReportGenerator.Timeframe.WEEKLY) set(Calendar.WEEK_OF_MONTH, selectedWeek)
                                    if (reportType == ReportGenerator.Timeframe.DAILY) set(Calendar.DAY_OF_MONTH, selectedDay)
                                }

                                val filteredData = filterExpensesByCustomDate(allExpenses, reportType, selectedMonth, selectedYear, selectedWeek, selectedDay)

                                if (filteredData.isEmpty()) {
                                    isGenerating = false
                                    android.widget.Toast.makeText(context, "No expenses logged for this period!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val periodLabel = getFormattedPeriodLabel(reportType, targetCal.timeInMillis)
                                val file = withContext(Dispatchers.IO) {
                                    ReportGenerator.generatePdfReport(context, filteredData, periodLabel, currentCurrency)
                                }

                                isGenerating = false
                                if (file != null) ReportGenerator.shareFile(context, file, "application/pdf")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Export PDF with Analytics", fontWeight = FontWeight.Bold)
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp), color = primaryColor, trackColor = primaryColor.copy(alpha = 0.2f))
                        Text("Generating professional report...", color = subTextColor, modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp)
                    }
                }
            }

            // ─────────────────────────────────────────
            // 4. SUPPORT & ABOUT
            // ─────────────────────────────────────────
            SettingsSectionHeader("Support & About", textColor)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineBorderColor)
            ) {
                Column {
                    SettingsRowAction(
                        icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Help Center", subtitle = "FAQs & Guide",
                        textColor = textColor, subTextColor = subTextColor, onClick = { showHelpDialog = true }
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f))
                    SettingsRowAction(
                        icon = Icons.Default.PrivacyTip, title = "Privacy Policy", subtitle = "Data protection & ML assurance",
                        textColor = textColor, subTextColor = subTextColor, onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = subTextColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("App Version", color = textColor, fontWeight = FontWeight.Medium)
                        }
                        Surface(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "v$appVersion",
                                color = primaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────
            // 5. DANGER ZONE
            // ─────────────────────────────────────────
            SettingsSectionHeader("Danger Zone", Color.Red)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (appearanceMode == "DARK") Color(0xFF450a0a).copy(alpha = 0.6f) else Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onResetGamification,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Gamification Progress", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe All Financial Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }


        // Validation Error Dialog
        if (showValidationErrorDialog) {
            val errorLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.error_alert))
            val errorLottie by errorLottieResult
            val lottieProgress by animateLottieCompositionAsState(composition = errorLottie, isPlaying = true, iterations = 1)
            AlertDialog(
                onDismissRequest = { showValidationErrorDialog = false },
                containerColor = cardColor,
                shape = RoundedCornerShape(28.dp),
                icon = {
                    if (errorLottie != null) { LottieAnimation(composition = errorLottie, progress = { lottieProgress }, modifier = Modifier.size(72.dp))
                    } else { Icon(Icons.Default.Warning, contentDescription = "Error", tint = errorColor, modifier = Modifier.size(48.dp)) }
                },
                title = { Text("Invalid Input", fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center) },
                text = { Text("Please check the highlighted fields and fix the errors before saving.", color = subTextColor, textAlign = TextAlign.Center) },
                confirmButton = {
                    Button(
                        onClick = { showValidationErrorDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Help Center Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = cardColor,
                shape = RoundedCornerShape(28.dp),
                title = {
                    Text(
                        "Help Center",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val helpText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: How does the AI Bandit work?\n")
                            }
                            append("A: Our Contextual AI learns your spending habits. If you spend erratically, it adapts by offering stricter advice. If you spend safely, it rewards you with streak bonuses. It learns every time you accept or ignore its advice!\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: How do I earn XP and level up?\n")
                            }
                            append("A: You earn XP by logging your expenses daily to keep your streak alive, and by following the AI's financial advice. Earning XP moves you up the tiers on the global leaderboard.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: Why did I receive a \"Strict Budget\" warning?\n")
                            }
                            append("A: The AI triggered this because it detected high \"Spending Volatility.\" This usually happens if you spend a large percentage of your monthly budget too quickly.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: Is my financial data secure?\n")
                            }
                            append("A: Yes. We do not connect to your bank or save any sensitive credentials. You appear on the leaderboard under a randomly generated anonymous name.")
                        }

                        Text(
                            text = helpText,
                            color = subTextColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHelpDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = cardColor,
                shape = RoundedCornerShape(28.dp),
                title = {
                    Text(
                        "Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val privacyText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Privacy by Design: Abstracted Reinforcement Learning\n\n")
                            }
                            append("Your privacy is our priority. Our AI learns from mathematical patterns, not personal identities.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("1. Financial Activity Data (The \"Context\")\n")
                            }
                            append("• We collect: Expense categories, transaction amounts, and timestamps.\n")
                            append("• Why: To provide the Contextual Multi-Armed Bandit (CMAB) AI with the context needed for personalized advice.\n")
                            append("• Note: We DO NOT connect to real bank accounts or collect sensitive banking credentials.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("2. AI Feedback Data (The \"Reward\")\n")
                            }
                            append("• We collect: The AI's predicted advice and your feedback score (+1.0 or -0.5).\n")
                            append("• Why: To close the Reinforcement Learning loop, allowing our algorithm to adjust and improve its accuracy over time.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("3. Gamification & Progression Data\n")
                            }
                            append("• We collect: An anonymous username, a randomized device/user ID, XP, and tier.\n")
                            append("• Why: To populate the global leaderboard and securely maintain your gamified progress without exposing your real identity.\n\n")

                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("4. Technical & Analytical Data\n")
                            }
                            append("• We collect: IP addresses and broad user segments.\n")
                            append("• Why: IPs are used strictly for secure server routing. User segments group similar behaviors for AI improvements without identifying individuals.")
                        }

                        Text(
                            text = privacyText,
                            color = subTextColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPrivacyDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("I Understand", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Wipe Data Confirmation Dialog
        if (showWipeDialog) {
            AlertDialog(
                onDismissRequest = { showWipeDialog = false },
                containerColor = cardColor,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Erase Everything?", fontWeight = FontWeight.Bold, color = textColor) },
                text = { Text("This will permanently delete all logged expenses from your local database. This action cannot be undone.", color = subTextColor) },
                confirmButton = {
                    Button(
                        onClick = { onWipeData(); showWipeDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes, Wipe Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeDialog = false }) {
                        Text("Cancel", color = textColor, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────
// HELPER COMPOSABLE & FUNCTIONS
// ─────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsRowToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, isChecked: Boolean,
    textColor: Color, subTextColor: Color, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = subTextColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, color = textColor, fontWeight = FontWeight.Medium); Text(subtitle, color = subTextColor, fontSize = 12.sp) }
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, textColor: Color, subTextColor: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = subTextColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = textColor, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) { Text(subtitle, color = subTextColor, fontSize = 12.sp) }
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = subTextColor)
    }
}

fun getFormattedPeriodLabel(type: ReportGenerator.Timeframe, timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val monthYearFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

    return when (type) {
        ReportGenerator.Timeframe.DAILY -> {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(calendar.time)
        }
        ReportGenerator.Timeframe.WEEKLY -> {
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
            "Week $weekOfMonth of ${monthYearFormat.format(calendar.time)}"
        }
        ReportGenerator.Timeframe.MONTHLY -> {
            monthYearFormat.format(calendar.time)
        }
    }
}

fun filterExpensesByCustomDate(
    allExpenses: List<ExpenseEntity>,
    type: ReportGenerator.Timeframe,
    targetMonth: Int,
    targetYear: Int,
    targetWeek: Int,
    targetDay: Int
): List<ExpenseEntity> {
    return allExpenses.filter { expense ->
        val expCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
        val matchesMonth = expCal.get(Calendar.MONTH) == targetMonth && expCal.get(Calendar.YEAR) == targetYear

        when (type) {
            ReportGenerator.Timeframe.DAILY -> matchesMonth && expCal.get(Calendar.DAY_OF_MONTH) == targetDay
            ReportGenerator.Timeframe.WEEKLY -> matchesMonth && expCal.get(Calendar.WEEK_OF_MONTH) == targetWeek
            ReportGenerator.Timeframe.MONTHLY -> matchesMonth
        }
    }
}
