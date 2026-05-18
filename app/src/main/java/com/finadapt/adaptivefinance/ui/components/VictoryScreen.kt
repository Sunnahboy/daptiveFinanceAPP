package com.finadapt.adaptivefinance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VictoryScreen(
    title: String = "Challenge Complete!",
    xpEarned: Int = 50,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // A glowing, animated success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("\uD83E\uDD70", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF10B981))
        Spacer(modifier = Modifier.height(8.dp))

        // The Dopamine Hit
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDC8E", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+$xpEarned XP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Return to Dashboard", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}