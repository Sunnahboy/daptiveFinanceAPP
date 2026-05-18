
package com.finadapt.adaptivefinance.ui.components

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.R
import kotlinx.coroutines.delay

@Composable
fun LevelUpOverlay(
    newTierName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    //The SoundPool
    DisposableEffect(Unit) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load the sound into memory
        soundPool.load(context, R.raw.level_up, 1)

        // Wait for it to finish loading, THEN play it
        soundPool.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0) { // 0 means it loaded successfully
                sp.play(sampleId, 1f, 1f, 1, 0, 1f)
            }
        }

        //Destroy the audio player the exact millisecond this overlay is dismissed
        onDispose {
            soundPool.release()
        }
    }

    //Auto-Dismiss Logic
    LaunchedEffect(Unit) {
        isVisible = true // Trigger the entrance animation
        delay(3500)
        isVisible = false // Trigger the exit animation
        delay(500) // Wait for exit animation to finish
        onDismiss() // Tell the ViewModel to close it completely
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)), // Dim the background
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //The Big Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉", fontSize = 40.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "LEVEL UP!",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You are now a",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )

                    Text(
                        text = newTierName,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Keep completing AI challenges to reach the next rank.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}