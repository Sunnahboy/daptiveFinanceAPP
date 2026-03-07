package com.finadapt.adaptivefinance.ui.components
import  androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import  androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
//1. The data structure for RPG Tiers
data class  UserTier(val name: String, val color: Color, val minXP: Int, val maxXp:Int)

@Composable
fun  GamifiedDashboardHeader(
    totalXp: Int,
    currentStreak: Int,
    userName: String,
) {
    //2 The Leveling Engine
    val currentTier = remember(totalXp) { calculateTier(totalXp) }
    //calculate how far along the user's xp is in the current tier
    val xpInCurrentTier = (totalXp - currentTier.minXP).coerceAtLeast(0)
    val tierSize = currentTier.maxXp - currentTier.minXP
    val targetProgress = if (tierSize > 0) xpInCurrentTier.toFloat() / tierSize.toFloat() else 1f

    //3.The animation state
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "xpProgress"
    )

    //4 . the dynamic greeting
    val greeting = remember { getDynamicGreeting(currentStreak) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ){
        Column(modifier = Modifier.padding(20.dp)){
            // top raw greeting and streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column {
                    Text(text = greeting, color = Color.Gray, fontSize = 14.sp)
                    Text(text = userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)

                }
                // fire Streak Badge
                if (currentStreak >= 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp)

                    ){
                        Text(
                            text = "\uD83D\uDD25 $currentStreak",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )

                    }
                }

            }

            Spacer(modifier = Modifier.height(24.dp))

        //--------middle row: rank  and XP math ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

            ){
                Text(text = currentTier.name, color = currentTier.color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "$totalXp / ${currentTier.maxXp} XP", color = Color.Gray, fontSize = 12.sp)

            }
            Spacer(modifier = Modifier.height(8.dp))

            //-----------Bottom row: The animated Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = currentTier.color,
                trackColor = Color(0xFF334155),
                strokeCap = StrokeCap.Round,
            )

        }
    }

}
    //--------Helper functions for the UI---------

    private fun calculateTier(xp: Int): UserTier{
        return when {
            xp < 500 -> UserTier("Bronze Novice",Color(0xFFCD7F32),0,500)
            xp < 2000 -> UserTier("Silver Guardian", Color(0xFF94A3B8), 500, 2000)
            xp < 5000 -> UserTier("Gold Master", Color(0xFFFBBF24), 2000, 5000)
            else -> UserTier("Platinum Legend", Color(0xFF06B6D4), 5000, 99999)
        }
    }



    private fun getDynamicGreeting(streak: Int): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timerGreeting = when (hour){
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening,"
        }

        //Mix in the gamification
        return  when {
            streak >= 7 -> "$timerGreeting, Streak Master"
            streak in 3..6 -> "$timerGreeting Keep it Up"
            else -> "$timerGreeting, Let's budget."
        }
    }







