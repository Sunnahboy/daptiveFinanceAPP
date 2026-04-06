package com.finadapt.adaptivefinance.feature.expense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    // Set up the Android Speech Recognizer
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try again."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error. Connect to internet."
                        else -> "Voice recognition error ($error)"
                    }
                    onError(errorMessage)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // Return the best match to the UI!
                        onResult(matches[0])
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    // Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(speechRecognizer, context)
            isListening = true
        } else {
            onError("Microphone permission denied.")
        }
    }

    // UI Animations (Pulses when listening!)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFFEF4444) else Color(0xFF0284C7),
        label = "color"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                if (isListening) {
                    speechRecognizer.stopListening()
                    isListening = false
                } else {
                    // Check Permission before starting
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        startListening(speechRecognizer, context)
                        isListening = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bgColor)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// Helper intent to start the recognizer
private fun startListening(recognizer: SpeechRecognizer, context: android.content.Context) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your expense...")
    }
    recognizer.startListening(intent)
}