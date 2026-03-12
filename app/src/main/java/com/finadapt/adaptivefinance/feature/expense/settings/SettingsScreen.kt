package com.finadapt.adaptivefinance.feature.expense.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentBudget: Float,
    isDarkMode: Boolean = false,
    onNameChanged: (String) -> Unit,
    onBudgetChanged: (Float) -> Unit,
    onThemeToggled: (Boolean) -> Unit,
    onResetGamification: () -> Unit,
    onWipeData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }
    var budgetInput by remember { mutableStateOf(currentBudget.toInt().toString()) }

    // 🟢 DIALOG STATES
    var showWipeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamic Colors based on Dark Mode state
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color.Gray
    val primaryColor = Color(0xFF0284C7)
    val successColor = Color(0xFF10B981)

    // Animation States
    var nameSaved by remember { mutableStateOf(false) }
    val nameBtnColor by animateColorAsState(targetValue = if (nameSaved) successColor else primaryColor, label = "nameColor")

    var budgetSaved by remember { mutableStateOf(false) }
    val budgetBtnColor by animateColorAsState(targetValue = if (budgetSaved) successColor else primaryColor, label = "budgetColor")

    // Get App Version from Package Manager
    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
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

            // --- 1. PROFILE & FINANCIAL BASELINE ---
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
                        onValueChange = { nameInput = it },
                        label = { Text("Preferred Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onNameChanged(nameInput)
                            keyboardController?.hide()
                            coroutineScope.launch {
                                nameSaved = true; delay(2000); nameSaved = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = nameBtnColor)
                    ) {
                        Text(if (nameSaved) "Saved! ✅" else "Save Name")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = subTextColor.copy(alpha = 0.2f))

                    Text("Monthly Budget Baseline", fontWeight = FontWeight.SemiBold, color = textColor)
                    Text("Used by the AI to calculate spending volatility.", color = subTextColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Budget Limit (RM)") },
                        prefix = { Text("RM ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onBudgetChanged(budgetInput.toFloatOrNull() ?: currentBudget)
                            keyboardController?.hide()
                            coroutineScope.launch {
                                budgetSaved = true; delay(2000); budgetSaved = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = budgetBtnColor)
                    ) {
                        Text(if (budgetSaved) "Updated! ✨" else "Update Budget")
                    }
                }
            }

            // --- 2. CUSTOMIZATION & PREFERENCES ---
            SettingsSectionHeader("Preferences", textColor)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsRowToggle(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Reduce eye strain",
                        isChecked = isDarkMode,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onCheckedChange = { onThemeToggled(it) }
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    SettingsRowAction(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "Manage streak and budget alerts",
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = {
                            // 🟢 WIRED: Opens Android System Notification Settings for this app!
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // --- 3. SUPPORT & ABOUT ---
            SettingsSectionHeader("Support & About", textColor)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsRowAction(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Help Center",
                        subtitle = "FAQs and contact support",
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = { showHelpDialog = true } // 🟢 WIRED
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    SettingsRowAction(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "How we protect your data",
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = { showPrivacyDialog = true } // 🟢 WIRED
                    )
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

            // --- 4. THE DANGER ZONE ---
            SettingsSectionHeader("Danger Zone", Color.Red)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF450a0a) else Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedButton(
                        onClick = onResetGamification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Reset Gamification Progress")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Wipe All Financial Data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
        }

        // 🟢 HELP CENTER DIALOG
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = cardColor,
                title = { Text("Help Center", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = """
                                Q: How does the AI Bandit work?
                                A: It uses Reinforcement Learning to suggest personalized financial interventions (like Zen, Strict Budget, or Quizzes) based on your recent spending habits.
                                
                                Q: What happens if I lose my streak?
                                A: If you forget to log an expense for a day, your streak will reset to zero unless you have an active Streak Shield (purchased with XP in the Rewards tab).
                                
                                Q: How do I earn Badges and XP?
                                A: You earn XP and unlock badges by consistently logging daily expenses, passing AI financial quizzes, and successfully completing budget lock challenges.
                                
                                Q: What is the Community Leaderboard?
                                A: It is an anonymous ranking system where you can compare your gamification progress (XP and Level) with other users. Your real name, balances, and expenses are NEVER shown.
                                
                                Q: Is my financial data safe?
                                A: Yes! Your data is synced securely to our cloud database using enterprise-grade Row Level Security (RLS), ensuring only your authenticated device can read your personal financial records.
                            """.trimIndent(),
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
                    ) { Text("Close") }
                }
            )
        }

        // 🟢 PRIVACY POLICY DIALOG
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                containerColor = cardColor,
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = """
                                1. Data Collection & Cloud Sync
                                To ensure you never lose your financial history, your expense logs and gamification progress are securely synchronized to our cloud database (powered by Supabase). 

                                2. Gamification & Leaderboard
                                To foster healthy financial habits, we feature an Anonymous Community Leaderboard. If you participate, only your current Gamification XP, Tier Rank, and a randomized username are visible to other users. Your actual financial balances and transactions are strictly hidden.

                                3. Security & Access
                                Your data is protected using enterprise-grade Row Level Security (RLS). This guarantees that your raw financial data is cryptographically isolated and can only be accessed by your authenticated device.
                                
                                4. Third-Party Sharing
                                We do not sell your personal data, email, or transaction history to advertisers or third-party brokers.
                            """.trimIndent(),
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
                    ) { Text("I Understand") }
                }
            )
        }

        // 🟢 WIPE DATA CONFIRMATION DIALOG
        if (showWipeDialog) {
            AlertDialog(
                onDismissRequest = { showWipeDialog = false },
                containerColor = cardColor,
                title = { Text("Erase Everything?", color = textColor) },
                text = { Text("This will permanently delete all logged expenses from your local database. This action cannot be undone.", color = subTextColor) },
                confirmButton = {
                    Button(
                        onClick = { onWipeData(); showWipeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Yes, Wipe Data") }
                },
                dismissButton = {
                    TextButton(onClick = { showWipeDialog = false }) {
                        Text("Cancel", color = textColor)
                    }
                }
            )
        }
    }
}

// --- HELPER COMPOSABLES ---

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
    title: String,
    subtitle: String,
    isChecked: Boolean,
    textColor: Color,
    subTextColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = subTextColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = textColor, fontWeight = FontWeight.Medium)
                Text(subtitle, color = subTextColor, fontSize = 12.sp)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7), checkedTrackColor = Color(0xFFE0F2FE))
        )
    }
}

@Composable
fun SettingsRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = subTextColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = textColor, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = subTextColor, fontSize = 12.sp)
                }
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = subTextColor)
    }
}