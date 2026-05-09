package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.finadapt.adaptivefinance.ui.components.SoundEngine


@Composable
fun GamificationDialog(
    action: String,
    message: String,
    predictionId: String,
    onFeedback: (String, Int) -> Unit, // Sends the 1 or 0 back to server
    onDismiss: () -> Unit              // Closes the router and goes back to Dashboard
) {
    val isDark = isSystemInDarkTheme()
    val dialogBgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val context = LocalContext.current
    val soundEngine = remember { SoundEngine(context) }

    //The Master Intercept Dialog
    Dialog(
        onDismissRequest = { /* We leave this empty to FORCE the user to interact with the game */ },
        properties = DialogProperties(
            dismissOnBackPress = false,       // Disables the Android Back Button
            dismissOnClickOutside = false     // Prevents tapping the background to escape
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            // The Game Router Logic
            when (action) {
                "quiz" -> QuizGame(
                    awsMessage = message,
                    soundEngine = soundEngine,
                    onGameComplete = { reward ->
                        onFeedback(predictionId, reward)
                        onDismiss()
                    }
                )

                "cool_off" -> CoolOffGame(
                    awsMessage = message,
                    soundEngine = soundEngine,
                    onGameComplete = { reward ->
                        onFeedback(predictionId, reward)
                        onDismiss()
                    }
                )

                "strict_budget" -> StrictBudgetGame(
                    awsMessage = message,
                    soundEngine = soundEngine,
                    onGameComplete = { reward ->
                        onFeedback(predictionId, reward)
                        onDismiss()
                    }
                )

                "streak_builder" -> StreakBuilderGame(
                    awsMessage = message,
                    soundEngine = soundEngine,
                    onGameComplete = { reward ->
                        onFeedback(predictionId, reward)
                        onDismiss()
                    }
                )

                // Fallback (Just in case bandit returns a prediction we haven't built yet)
                else -> {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("🤖 AI Intervention", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(message, color = if (isDark) Color.LightGray else Color.DarkGray)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                onFeedback(predictionId, 1)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge")
                        }
                    }
                }
            }
        }
    }
}