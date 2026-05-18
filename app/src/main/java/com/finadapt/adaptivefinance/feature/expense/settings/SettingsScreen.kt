package com.finadapt.adaptivefinance.feature.expense.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentBudget: Float,
    isDarkMode: Boolean = false,
    allExpenses: List<ExpenseEntity>,
    onNameChanged: (String) -> Unit,
    onBudgetChanged: (Float) -> Unit,
    onThemeToggled: (Boolean) -> Unit,
    onResetGamification: () -> Unit,
    onWipeData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }

    // Using simple format to avoid trailing .0 if not needed
    val initialBudgetString = if (currentBudget % 1 == 0f) currentBudget.toInt().toString() else currentBudget.toString()
    var budgetInput by remember { mutableStateOf(initialBudgetString) }

    var reminderTimes by remember { mutableStateOf(NotificationScheduler.getTimesFromPrefs(context)) }

    // INDIVIDUAL BUTTON STATES
    var nameActionState by remember { mutableStateOf("IDLE") }
    var budgetActionState by remember { mutableStateOf("IDLE") }

    // EXPORT STATES
    var isGenerating by remember { mutableStateOf(false) }
    var reportType by remember { mutableStateOf(ReportGenerator.Timeframe.MONTHLY) } //Using Enum again

    val currentCal = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedWeek by remember { mutableIntStateOf(currentCal.get(Calendar.WEEK_OF_MONTH)) }
    var selectedDay by remember { mutableIntStateOf(currentCal.get(Calendar.DAY_OF_MONTH)) } // Day Tracker

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
    val coroutineScope = rememberCoroutineScope()

    val successComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success_check))
    val infoComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.nothing_changed))

    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val primaryColor = Color(0xFF0284C7)
    val successColor = Color(0xFF10B981)
    val infoColor = Color(0xFF64748B)
    val errorColor = Color(0xFFEF4444)

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    // ================= REAL-TIME VALIDATION LOGIC =================

    // Name Validation
    val cleanName = nameInput.trim()
    val isNameError = cleanName.isEmpty() || cleanName.length > 15
    val nameErrorMessage = when {
        cleanName.isEmpty() -> "Name cannot be empty."
        cleanName.length > 15 -> "Keep it short (max 15 characters)."
        else -> null
    }

    // Budget Validation
    val parsedBudget = budgetInput.toFloatOrNull()
    val isBudgetError = budgetInput.isNotEmpty() && (parsedBudget == null || parsedBudget <= 0f || parsedBudget > 1_000_000f)
    val budgetErrorMessage = when {
        budgetInput.isEmpty() -> "Budget cannot be empty."
        parsedBudget == null || parsedBudget <= 0f -> "Enter a valid amount greater than 0."
        parsedBudget > 1_000_000f -> "Budget exceeds maximum limit."
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ─────────────────────────────────────────
            // 1. ACCOUNT & PROFILE
            // ─────────────────────────────────────────
            SettingsSectionHeader("Account & Profile", textColor)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { newValue ->
                            // SANITIZATION: Actively strip out numbers, dots, commas, and emojis.
                            // Only allow letters, spaces, hyphens, and apostrophes.
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                        enabled = !isNameError, // Prevents submission of invalid data
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = when (nameActionState) { "SUCCESS" -> successColor; "NO_CHANGE" -> infoColor; else -> primaryColor })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (nameActionState) {
                                "SUCCESS" -> { LottieAnimation(successComp, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Saved!", color = Color.White) }
                                "NO_CHANGE" -> { LottieAnimation(infoComp, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Already set", color = Color.White) }
                                else -> Text("Save Name", color = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = subTextColor.copy(alpha = 0.2f))

                    Text("Monthly Budget Baseline", fontWeight = FontWeight.SemiBold, color = textColor)
                    Text("Used by the AI to calculate spending volatility.", color = subTextColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { newValue ->
                            //Fix European commas and filter out bad chars instantly
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
                        label = { Text("Budget Limit (RM)") },
                        prefix = { Text("RM ", color = textColor) },
                        singleLine = true,
                        isError = isBudgetError || budgetInput.isEmpty(),
                        supportingText = {
                            if (budgetErrorMessage != null) Text(budgetErrorMessage, color = errorColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        // Add this inside BOTH OutlinedTextFields (Name and Budget):
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                        enabled = !isBudgetError && budgetInput.isNotEmpty(), // Blocks null/negative saves
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = when (budgetActionState) { "SUCCESS" -> successColor; "NO_CHANGE" -> infoColor; else -> primaryColor })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (budgetActionState) {
                                "SUCCESS" -> { LottieAnimation(successComp, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Updated!", color = Color.White) }
                                "NO_CHANGE" -> { LottieAnimation(infoComp, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("No change", color = Color.White) }
                                else -> Text("Update Budget", color = Color.White)
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
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsRowToggle(
                        icon = Icons.Default.DarkMode, title = "Dark Mode", subtitle = "Reduce eye strain",
                        isChecked = isDarkMode, textColor = textColor, subTextColor = subTextColor,
                        onCheckedChange = { onThemeToggled(it) }
                    )

                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))

                    // THE NOTIFICATION TIMES UI
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daily Reminders", color = textColor, fontWeight = FontWeight.Medium)
                                Text("When should the AI remind you?", color = subTextColor, fontSize = 12.sp)
                            }
                            // Add Time Button
                            if (reminderTimes.size < 3) { // limit to 3 reminders max to prevent spam
                                IconButton(onClick = {
                                    val calendar = Calendar.getInstance()
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newTimes = reminderTimes.toMutableList()
                                            newTimes.add(Pair(hourOfDay, minute))
                                            reminderTimes = newTimes

                                            // Save & Reschedule!
                                            NotificationScheduler.saveTimesToPrefs(context, newTimes)
                                            NotificationScheduler.scheduleDailyReminders(context, newTimes)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Time", tint = primaryColor)
                                }
                            }
                        }

                        // Display the chosen times
                        Spacer(modifier = Modifier.height(8.dp))
                        reminderTimes.forEachIndexed { index, time ->
                            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, time.first); set(Calendar.MINUTE, time.second) }
                            val timeFormatted = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = subTextColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(timeFormatted, color = textColor, fontWeight = FontWeight.Bold)
                                }

                                // Delete Button
                                IconButton(onClick = {
                                    val newTimes = reminderTimes.toMutableList()
                                    newTimes.removeAt(index)
                                    reminderTimes = newTimes

                                    // Save and  Reschedule
                                    NotificationScheduler.saveTimesToPrefs(context, newTimes)
                                    NotificationScheduler.scheduleDailyReminders(context, newTimes)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    SettingsRowAction(
                        icon = Icons.Default.Notifications, title = "Push Notifications", subtitle = "Manage alerts",
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
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // TYPE TOGGLE (Now has 3 options)
                    Text("Select Timeframe:", color = subTextColor, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportGenerator.Timeframe.entries.forEach { timeframe ->
                            FilterChip(
                                selected = reportType == timeframe,
                                onClick = { reportType = timeframe },
                                label = { Text(timeframe.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    // MONTH DROPDOWN (Always visible)
                    ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = !monthExpanded }) {
                        OutlinedTextField(
                            value = "${monthsList[selectedMonth]} $selectedYear",
                            onValueChange = {}, readOnly = true, label = { Text("Select Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor, unfocusedTextColor = textColor,
                                focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.5f)
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

                    // WEEK DROPDOWN (Only visible if Weekly is selected)
                    if (reportType == ReportGenerator.Timeframe.WEEKLY) {
                        ExposedDropdownMenuBox(expanded = weekExpanded, onExpandedChange = { weekExpanded = !weekExpanded }) {
                            OutlinedTextField(
                                value = "Week $selectedWeek",
                                onValueChange = {}, readOnly = true, label = { Text("Select Week") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                                    focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.5f)
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

                    //DAY DROPDOWN (Only visible if Daily is selected)
                    if (reportType == ReportGenerator.Timeframe.DAILY) {
                        ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = !dayExpanded }) {
                            OutlinedTextField(
                                value = "Day $selectedDay",
                                onValueChange = {}, readOnly = true, label = { Text("Select Day") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textColor, unfocusedTextColor = textColor,
                                    focusedBorderColor = primaryColor, unfocusedBorderColor = subTextColor.copy(alpha = 0.5f)
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

                    //EXCEL EXPORT
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                // 1. Map choices to the calendar
                                val targetCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    if (reportType == ReportGenerator.Timeframe.WEEKLY) set(Calendar.WEEK_OF_MONTH, selectedWeek)
                                    if (reportType == ReportGenerator.Timeframe.DAILY) set(Calendar.DAY_OF_MONTH, selectedDay)
                                }

                                // 2. Filter data
                                val filteredData = filterExpensesByCustomDate(allExpenses, reportType, selectedMonth, selectedYear, selectedWeek, selectedDay)

                                // 3. 🟢 THE SAFETY CHECK: Stop if no data!
                                if (filteredData.isEmpty()) {
                                    isGenerating = false
                                    android.widget.Toast.makeText(context, "No expenses logged for this period!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch // Abort generation
                                }

                                // 4. Generate report
                                val periodLabel = getFormattedPeriodLabel(reportType, targetCal.timeInMillis)
                                val file = withContext(Dispatchers.IO) {
                                    ReportGenerator.generateExcelReport(context, filteredData, periodLabel)
                                }

                                isGenerating = false
                                if (file != null) ReportGenerator.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = successColor)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Export as Excel (.xlsx)")
                    }

                    //PDF EXPORT
                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                // 1. Map choices to the calendar
                                val targetCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    if (reportType == ReportGenerator.Timeframe.WEEKLY) set(Calendar.WEEK_OF_MONTH, selectedWeek)
                                    if (reportType == ReportGenerator.Timeframe.DAILY) set(Calendar.DAY_OF_MONTH, selectedDay)
                                }

                                // 2. Filter data
                                val filteredData = filterExpensesByCustomDate(allExpenses, reportType, selectedMonth, selectedYear, selectedWeek, selectedDay)

                                // 3. THE SAFETY CHECK: Stop if no data
                                if (filteredData.isEmpty()) {
                                    isGenerating = false
                                    android.widget.Toast.makeText(context, "No expenses logged for this period!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch // Abort generation
                                }

                                // 4. Generate report
                                val periodLabel = getFormattedPeriodLabel(reportType, targetCal.timeInMillis)
                                val file = withContext(Dispatchers.IO) {
                                    ReportGenerator.generatePdfReport(context, filteredData, periodLabel)
                                }

                                isGenerating = false
                                if (file != null) ReportGenerator.shareFile(context, file, "application/pdf")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Export PDF with Analytics")
                    }

                    if (isGenerating) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primaryColor)
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
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsRowAction(
                        icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Help Center", subtitle = "FAQs",
                        textColor = textColor, subTextColor = subTextColor, onClick = { showHelpDialog = true }
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    SettingsRowAction(
                        icon = Icons.Default.PrivacyTip, title = "Privacy Policy", subtitle = "Data protection",
                        textColor = textColor, subTextColor = subTextColor, onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = subTextColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("App Version", color = textColor, fontWeight = FontWeight.Medium)
                        }
                        Text("v$appVersion", color = subTextColor, fontSize = 14.sp)
                    }
                }
            }

            // ─────────────────────────────────────────
            // 5. DANGER ZONE
            // ─────────────────────────────────────────
            SettingsSectionHeader("Danger Zone", Color.Red)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF450a0a) else Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedButton(
                        onClick = onResetGamification, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) { Text("Reset Gamification Progress") }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showWipeDialog = true }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Wipe All Financial Data", color = Color.White) }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }


        // Validation Error Dialog (Kept as a fallback, though inline validation prevents it from being needed)
        if (showValidationErrorDialog) {
            val errorLottieResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.error_alert))
            val errorLottie by errorLottieResult
            val lottieProgress by animateLottieCompositionAsState(composition = errorLottie, isPlaying = true, iterations = 1)
            AlertDialog(
                onDismissRequest = { showValidationErrorDialog = false },
                containerColor = cardColor,
                icon = {
                    if (errorLottie != null) { LottieAnimation(composition = errorLottie, progress = { lottieProgress }, modifier = Modifier.size(72.dp))
                    } else { Icon(Icons.Default.Warning, contentDescription = "Error", tint = errorColor, modifier = Modifier.size(48.dp)) }
                },
                title = { Text("Invalid Input", fontWeight = FontWeight.Bold, color = textColor) },
                text = { Text("Please check the highlighted fields and fix the errors before saving.", color = subTextColor, textAlign = TextAlign.Center) },
                confirmButton = {
                    Button(onClick = { showValidationErrorDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor), modifier = Modifier.fillMaxWidth()) { Text("Got it", color = Color.White) }
                }
            )
        }

        // Help Center Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = cardColor,
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
                            .heightIn(max = 400.dp) //Keeps the dialog scrollable
                            .verticalScroll(rememberScrollState())
                    ) {
                        val helpText = buildAnnotatedString {
                            // Question 1
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: How does the AI Bandit work?\n")
                            }
                            append("A: Our Contextual AI learns your spending habits. If you spend erratically, it adapts by offering stricter advice. If you spend safely, it rewards you with streak bonuses. It learns every time you accept or ignore its advice!\n\n")

                            // Question 2
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: How do I earn XP and level up?\n")
                            }
                            append("A: You earn XP by logging your expenses daily to keep your streak alive, and by following the AI's financial advice. Earning XP moves you up the tiers on the global leaderboard.\n\n")

                            // Question 3
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Q: Why did I receive a \"Strict Budget\" warning?\n")
                            }
                            append("A: The AI triggered this because it detected high \"Spending Volatility.\" This usually happens if you spend a large percentage of your monthly budget too quickly.\n\n")

                            // Question 4
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
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = cardColor,
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
                            .heightIn(max = 400.dp) //Keeps the dialog from taking up the whole screen
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Build the text with bold headers
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
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("I Understand", color = Color.White)
                    }
                }
            )
        }

        // Wipe Data Confirmation Dialog
        if (showWipeDialog) {
            AlertDialog(
                onDismissRequest = { showWipeDialog = false },
                containerColor = cardColor,
                title = { Text("Erase Everything?", color = textColor) },
                text = { Text("This will permanently delete all logged expenses from your local database. This action cannot be undone.", color = subTextColor) },
                confirmButton = { Button(onClick = { onWipeData(); showWipeDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Yes, Wipe Data", color = Color.White) } },
                dismissButton = { TextButton(onClick = { showWipeDialog = false }) { Text("Cancel", color = textColor) } }
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
        color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRowToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, isChecked: Boolean,
    textColor: Color, subTextColor: Color, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!isChecked) }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = subTextColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(title, color = textColor, fontWeight = FontWeight.Medium); Text(subtitle, color = subTextColor, fontSize = 12.sp) }
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7), checkedTrackColor = Color(0xFFE0F2FE)))
    }
}

@Composable
fun SettingsRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, textColor: Color, subTextColor: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
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

// Formatting the custom string for the ReportGenerator
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

// slicing the database based on UI Dropdown choices
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