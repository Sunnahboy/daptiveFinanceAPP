package com.finadapt.adaptivefinance.feature.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ThemePrimary = Color(0xFF30E87A)
val ThemeBgLight = Color(0xFFF6F8F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    uiState: GamificationUiState, // 🟢 NEW: Listens to the ViewModel
    onLogExpense: (amount: Double, category: String) -> Unit,
    onFeedback: (predictionId: String, reward: Float) -> Unit, // 🟢 NEW: Sends reward to Python
    onDismissState: () -> Unit // 🟢 NEW: Resets the UI
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }

    val categories = listOf(
        "Food" to "🍔",
        "Transport" to "🚗",
        "Entertainment" to "🎬",
        "Bills" to "💡"
    )

    // 🟢 The AI Pop-up (Bottom Sheet)
    // 🟢 The AI Pop-up (Bottom Sheet)
    if (uiState is GamificationUiState.Success) {
        // Track if they voted so we don't accidentally send duplicate feedback
        var hasVoted by remember(uiState.predictionId) { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = {
                // If they swipe away without voting, we just close it.
                // We don't punish the AI, because they might have just been in a rush.
                onDismissState()
            },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✨ AI Gamification",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // The AI Message
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 🟢 NEW: Explicit Feedback Row
                Text(
                    text = "Was this helpful?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 👎 Negative Feedback Button
                    OutlinedButton(
                        onClick = {
                            if (!hasVoted) {
                                hasVoted = true
                                onFeedback(uiState.predictionId, 0.0f) // Tell AI to stop doing this
                                println("📉 User voted: Not Helpful (0.0)")
                                onDismissState()
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Not Really 👎", fontWeight = FontWeight.Bold)
                    }

                    // 👍 Positive Feedback Button
                    Button(
                        onClick = {
                            if (!hasVoted) {
                                hasVoted = true
                                onFeedback(uiState.predictionId, 1.0f) // Tell AI to keep doing this!
                                println("📈 User voted: Helpful (1.0)")
                                onDismissState()
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                    ) {
                        Text("Yes, Thanks! 👍", color = Color(0xFF022C22), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    // ... (KEEP YOUR EXISTING SURFACE AND COLUMN UI EXACTLY THE SAME HERE)
    Surface(modifier = Modifier.fillMaxSize(), color = ThemeBgLight) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))
            Text("Log Expense", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(32.dp))
            Text("Amount (RM)", color = Color.Gray, style = MaterialTheme.typography.labelLarge)

            TextField(
                value = amountInput,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amountInput = it },
                textStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF0F172A)),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                placeholder = { Text("0.00", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 56.sp, color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))
            Text("SELECT CATEGORY", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 24.dp).align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 24.dp)) {
                items(categories) { (name, emoji) ->
                    val isSelected = selectedCategory == name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = name },
                        label = { Text("$emoji  $name", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 16.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD1FAE5), selectedLabelColor = Color(0xFF064E3B), containerColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if (isSelected) ThemePrimary else Color(0xFFE2E8F0), borderWidth = if (isSelected) 2.dp else 1.dp),
                        modifier = Modifier.height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Check if UI is loading
            if (uiState is GamificationUiState.Loading) {
                CircularProgressIndicator(color = ThemePrimary)
            } else {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onLogExpense(amount, selectedCategory)
                            amountInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(24.dp).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                    shape = RoundedCornerShape(50),
                    enabled = amountInput.isNotBlank() && (amountInput.toDoubleOrNull()
                        ?: 0.0) > 0.0
                ) {
                    Text("Log & Analyze 🚀", color = Color(0xFF022C22), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}