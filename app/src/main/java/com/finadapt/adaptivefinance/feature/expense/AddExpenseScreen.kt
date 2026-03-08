package com.finadapt.adaptivefinance.feature.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.feature.gamification.GamificationDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    uiState: GamificationUiState,
    onLogExpense: (Float, String) -> Unit,
    onFeedback: (String, Int) -> Unit,
    onDismissState: () -> Unit,

) {
    var amountInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }

    val commonCategories = listOf("Food", "Transport", "Groceries", "Coffee", "Entertainment")
    val keyboardController = LocalSoftwareKeyboardController.current

    val bgColor = Color(0xFFF8FAFC)
    val cardColor = Color.White
    val primaryColor = Color(0xFF0284C7)

    val isValid = amountInput.isNotBlank() && categoryInput.isNotBlank() && amountInput.toFloatOrNull() != null
    val isLoading = uiState is GamificationUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismissState) {
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 🟢 1. AMOUNT INPUT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("How much did you spend?", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        prefix = { Text("RM ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = primaryColor) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        )
                    )
                }
            }

            // 🟢 2. CATEGORY SELECTION
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

            OutlinedTextField(
                value = categoryInput,
                onValueChange = { categoryInput = it },
                placeholder = { Text("e.g. Starbucks, Uber, Rent") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonCategories.forEach { category ->
                    val isSelected = categoryInput == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { categoryInput = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor.copy(alpha = 0.1f),
                            selectedLabelColor = primaryColor
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 🟢 3. ERROR MESSAGE (If AWS API fails)
            if (uiState is GamificationUiState.Error) {
                Text(
                    text = uiState.exception,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // 🟢 4. THE SMART AI BUTTON
            Button(
                onClick = {
                    if (isValid) {
                        keyboardController?.hide()
                        onLogExpense(amountInput.toFloat(), categoryInput) // Send to AWS!
                    }
                },
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(30.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Calculating...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("Log Expense", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 🎮 5. THE GAMIFICATION ENGINE (Clean and Modular!)
        if (uiState is GamificationUiState.Success) {
            if (uiState.action == "Log_Only") {
                // No game required! Instantly return to Dashboard.
                LaunchedEffect(Unit) {
                    onDismissState()
                }
            } else {
                // 🚨 Trigger the Master Game Router!
                GamificationDialog(
                    action = uiState.action,
                    message = uiState.message,
                    predictionId = uiState.predictionId,
                    onFeedback = { predId, reward ->
                        onFeedback(predId, reward)
                    },
                    onDismiss = {
                        onDismissState()
                    }
                )
            }
        }
    }
}