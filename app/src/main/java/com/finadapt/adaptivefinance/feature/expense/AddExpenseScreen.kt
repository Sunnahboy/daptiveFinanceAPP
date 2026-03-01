package com.finadapt.adaptivefinance.feature.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel




@Composable
fun AddExpenseScreen(viewModel: ExpenseViewModel = viewModel()){
    // Listen to the ViewModel's state
    val uiState by viewModel.uiState.collectAsState()
    var amount by remember { mutableStateOf("") }

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Adaptive Finance AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Enter Expense Amount (RM)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick =  {viewModel.submitExpense(amount)},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = uiState !is GamificationUiState.Loading && amount.isNotBlank()
        ) {
            if (uiState is  GamificationUiState.Loading){
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Log Expense & fetch AI Strategy")

            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        //Displaying the AI Response from AWS
        when (val state = uiState){
            is GamificationUiState.Success ->{
                // 1. Calculate the dynamic color
                val dynamicColor = when (state.visualTheme.lowercase()){
                    "warning","alert","danger" -> MaterialTheme.colorScheme.errorContainer
                    "success","reward","positive"-> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }

                Card(
                    // 2. USE the dynamic color we just calculated!
                    colors = CardDefaults.cardColors(containerColor = dynamicColor),
                    modifier = Modifier.fillMaxWidth()
                ){
                    Column (modifier = Modifier.padding(16.dp)){
                        Text(
                            text = "\uD83C\uDFAF AI Action: ${state.action.replace("_", " ").uppercase()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\uD83E\uDDE0 Bandit Strategy: ${state.strategy}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        // 🟢 3. THE MISSING REWARD BUTTON!
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                println("🎯 USER CLICKED! Sending Reward...")
                                // Tell the ViewModel the user engaged!
                                viewModel.submitFeedback(state.predictionId, 1.0f)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Got it! \uD83D\uDE80") // Rocket emoji
                        }
                    }
                }
            }
            is GamificationUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: ${state.exception}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {} //Do nothing if idle
        }
    }
}
