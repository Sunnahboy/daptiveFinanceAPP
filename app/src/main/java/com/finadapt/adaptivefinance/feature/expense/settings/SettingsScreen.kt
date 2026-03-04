package com.finadapt.adaptivefinance.feature.expense.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentBudget: Float,
    onNameChanged: (String) -> Unit,
    onBudgetChanged: (Float) -> Unit,
    onResetGamification: () -> Unit,
    onWipeData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var budgetInput by remember { mutableStateOf(currentBudget.toInt().toString()) }
    var showWipeDialog by remember { mutableStateOf(false) }

    // 🟢 NEW: Tools for the animations
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val bgColor = Color(0xFFF8FAFC)
    val cardColor = Color.White
    val textColor = Color(0xFF0F172A)
    val primaryColor = Color(0xFF0284C7) // Ocean Blue
    val successColor = Color(0xFF10B981) // Emerald Green

    // 🟢 NEW: Animation States
    var nameSaved by remember { mutableStateOf(false) }
    val nameBtnColor by animateColorAsState(targetValue = if (nameSaved) successColor else primaryColor, label = "nameColor")

    var budgetSaved by remember { mutableStateOf(false) }
    val budgetBtnColor by animateColorAsState(targetValue = if (budgetSaved) successColor else primaryColor, label = "budgetColor")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 🟢 1. PERSONALIZATION CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
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
                            keyboardController?.hide() // Hide the keyboard!
                            coroutineScope.launch {
                                nameSaved = true
                                delay(2000) // Show success for 2 seconds
                                nameSaved = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = nameBtnColor)
                    ) {
                        Text(if (nameSaved) "Saved! ✅" else "Save Name")
                    }
                }
            }

            // 🟢 2. FINANCIAL BASELINE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Financial Baseline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The AI Bandit uses this baseline to calculate your spending volatility.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly Budget (RM)") },
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
                            keyboardController?.hide() // Hide the keyboard!
                            coroutineScope.launch {
                                budgetSaved = true
                                delay(2000) // Show success for 2 seconds
                                budgetSaved = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = budgetBtnColor)
                    ) {
                        Text(if (budgetSaved) "Updated! ✨" else "Update Budget")
                    }
                }
            }

            // 🟢 3. THE DANGER ZONE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onResetGamification,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Reset XP & Gamification Level")
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
        }

        // 🟢 CONFIRMATION DIALOG
        if (showWipeDialog) {
            AlertDialog(
                onDismissRequest = { showWipeDialog = false },
                title = { Text("Erase Everything?") },
                text = { Text("This will permanently delete all logged expenses from your local database. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onWipeData()
                            showWipeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes, Wipe Data")
                    }
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