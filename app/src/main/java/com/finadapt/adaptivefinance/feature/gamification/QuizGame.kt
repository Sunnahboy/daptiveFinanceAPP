package com.finadapt.adaptivefinance.feature.gamification

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.ui.components.SoundEngine
import kotlinx.coroutines.delay

enum class QuizPhase { INTRO, PLAYING, RESULT }

// 🟢 Local Data Pool
val financialTrivia = listOf(
    Pair("What is the 50/30/20 budget rule?", listOf("Needs/Wants/Savings", "Savings/Needs/Wants", "Wants/Savings/Needs")),
    Pair("Which category drains a budget fastest due to 'invisible' spending?", listOf("Dining Out & Subscriptions", "Rent & Utilities", "Car Insurance")),
    Pair("What is an emergency fund typically supposed to cover?", listOf("3-6 months of expenses", "1 month of expenses", "A down payment"))
)

private const val QUIZ_TIME_LIMIT = 15

@Composable
fun QuizGame(
    awsMessage: String,
    soundEngine: SoundEngine, // 🟢 Inject Sound Engine
    onGameComplete: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color.Gray
    val brandPurple = Color(0xFFA78BFA)

    // Game State
    var currentPhase by remember { mutableStateOf(QuizPhase.INTRO) }
    // 🔴 FIX #1: Changed val to var (should use var with remember)
    var currentQuestion by remember { mutableStateOf(financialTrivia.random()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    val correctAnswer = currentQuestion.second[0]
    val shuffledOptions by remember { mutableStateOf(currentQuestion.second.shuffled()) }

    // 🟢 The Timer State (15 Seconds)
    var timeLeft by remember { mutableIntStateOf(QUIZ_TIME_LIMIT) }
    var isTimeUp by remember { mutableStateOf(false) }

    // Smooth shrinking animation for the timer bar
    // 🔴 FIX #2: Use toFloat() to ensure proper division
    val animatedProgress by animateFloatAsState(targetValue = timeLeft.toFloat() / QUIZ_TIME_LIMIT, label = "timerBar")

    //If the game gets closed unexpectedly, shut off the sound!
    DisposableEffect(Unit) {
        onDispose {
            soundEngine.stopAllTicking()
        }
    }

    // 🟢 The Timer Logic & Escalating Audio Tension (Fixed!)
    LaunchedEffect(currentPhase) {
        if (currentPhase == QuizPhase.PLAYING) {
            // 🔴 FIX #3: Reset timer when entering PLAYING phase
            timeLeft = QUIZ_TIME_LIMIT

            while (timeLeft > 0) {
                delay(1000)

                // 🛑 CRITICAL FIX: If they clicked an answer during that 1-second delay,
                // we instantly break out of the loop BEFORE the next tick plays!
                if (currentPhase != QuizPhase.PLAYING) break

                timeLeft--

                // 🎵 The Dynamic Audio Escalation
                when (timeLeft) {
                    //in 4..5 -> soundEngine.play("tick2") // Standard clock tick
                    in 1..3 -> soundEngine.play("tick1") // Urgent bell tick-tock!
                }
            }

            // If the loop finished naturally (hit 0) AND they never picked an answer:
            if (timeLeft == 0 && selectedAnswer == null) {
                isTimeUp = true
                soundEngine.stopAllTicking() // 🛑 KILL THE TICKING!
                soundEngine.play("lose") // 🔊 Play fail sound
                currentPhase = QuizPhase.RESULT
            }
        }
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🧠 Financial IQ Test", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = brandPurple)
        Spacer(modifier = Modifier.height(16.dp))

        when (currentPhase) {

            // 🟢 PHASE 1: INTRO
            QuizPhase.INTRO -> {
                Text(awsMessage, textAlign = TextAlign.Center, color = subTextColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            soundEngine.play("lose")
                            onGameComplete(0)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline", color = subTextColor)
                    }
                    Button(
                        onClick = {
                            soundEngine.play("click")
                            currentPhase = QuizPhase.PLAYING
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Quiz", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 🟢 PHASE 2: PLAYING (The Timed Challenge)
            QuizPhase.PLAYING -> {
                // The Shrinking Timer Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (timeLeft > 5) brandPurple else Color.Red, // Turns red at 5 seconds!
                    trackColor = subTextColor.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("00:${timeLeft.toString().padStart(2, '0')}", color = if (timeLeft > 5) subTextColor else Color.Red, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))

                Text(currentQuestion.first, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, color = textColor)
                Spacer(modifier = Modifier.height(24.dp))

                shuffledOptions.forEach { option ->
                    Button(
                        onClick = {
                            selectedAnswer = option
                            soundEngine.stopAllTicking()
                            val isCorrect = selectedAnswer == correctAnswer
                            if (isCorrect) soundEngine.play("success") else soundEngine.play("lose")
                            currentPhase = QuizPhase.RESULT
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(option, color = Color.White, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            // 🟢 PHASE 3: RESULT (Micro-Celebration or Failure)
            QuizPhase.RESULT -> {
                val isCorrect = selectedAnswer == correctAnswer

                if (isTimeUp) {
                    Text("⏳ Time's Up!", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You took too long to answer.", color = subTextColor)
                } else if (isCorrect) {
                    // 🌟 Use the Victory Screen pattern here!
                    Text("✨ Correct!", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), shape = RoundedCornerShape(12.dp)) {
                        Text("💎 +50 XP", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                    }
                } else {
                    Text("❌ Incorrect", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("The correct answer was:\n$correctAnswer", textAlign = TextAlign.Center, color = subTextColor)
                }

                Spacer(modifier = Modifier.height(32.dp))

                val buttonColor = if (isCorrect && !isTimeUp) Color(0xFF10B981) else Color.Gray
                Button(
                    onClick = { onGameComplete(if (isCorrect && !isTimeUp) 1 else 0) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text("Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}