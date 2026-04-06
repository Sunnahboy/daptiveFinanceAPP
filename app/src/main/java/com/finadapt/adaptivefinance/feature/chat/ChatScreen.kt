package com.finadapt.adaptivefinance.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 🟢 NEW: Our Predefined Smart Questions
    val suggestedQuestions = listOf(
        "What did I spend today?",
        "What was my highest purchase?",
        "Where am I spending the most?",
        "What is my remaining budget?"
    )

    // Theming Colors
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val surfaceColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    // Auto-scroll to the bottom whenever a new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Adaptive AI", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color.Green, RoundedCornerShape(4.dp)))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = bgColor,
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. THE CHAT HISTORY
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message, isDark = isDark)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp))
                                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }
            }

            // 🟢 2. THE SUGGESTION CHIPS (Predefined Questions)
            // They gracefully disappear if the user starts typing manually
            AnimatedVisibility(
                visible = inputText.isEmpty(),
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedQuestions) { question ->
                        Surface(
                            onClick = {
                                if (!isLoading) {
                                    viewModel.sendMessage(question)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE0F2FE),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = question,
                                    color = if (isDark) Color.White else Color(0xFF0284C7),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. THE INPUT BAR
            Surface(
                color = surfaceColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about your spending...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0284C7), RoundedCornerShape(24.dp)),
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    var textToDisplay by remember { mutableStateOf("") }

    // 🟢 CRITICAL: rememberSaveable ensures that once a message finishes typing,
    // it stays fully typed even if the user scrolls it off the screen and back.
    var animationFinished by androidx.compose.runtime.saveable.rememberSaveable(text) { mutableStateOf(false) }

    LaunchedEffect(text) {
        if (!animationFinished) {
            // Speed: 15 milliseconds per character is a very natural reading speed!
            for (i in text.indices) {
                textToDisplay = text.substring(0, i + 1)
                kotlinx.coroutines.delay(15)
            }
            animationFinished = true
        } else {
            textToDisplay = text
        }
    }

    Text(
        text = textToDisplay,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

// 🟢 Custom UI Component for the Chat Bubbles
@Composable
fun ChatBubble(message: ChatMessage, isDark: Boolean) {
    val isUser = message.isFromUser

    // User messages go on the right, AI on the left
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    // User gets a sharp bottom-right corner, AI gets a sharp bottom-left
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    // Colors
    val bubbleColor = if (isUser) {
        Color(0xFF0284C7) // Adaptive Finance Blue
    } else {
        if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0) // Gray for AI
    }

    val textColor = if (isUser) Color.White else (if (isDark) Color.White else Color.Black)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            if (isUser) {
                // User messages appear instantly
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            } else {
                // AI messages get the cool Typewriter Effect!
                TypewriterText(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }


}




@Composable
fun DraggableAiChatFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 🟢 CALL TO ACTION LOGIC
    val fullText = "Ask AI..."
    var displayedText by remember { mutableStateOf("") }
    var isLabelVisible by remember { mutableStateOf(false) } // Start hidden

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500) // Wait for dashboard to settle
        isLabelVisible = true
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            kotlinx.coroutines.delay(80)
        }
        kotlinx.coroutines.delay(4000) // Show for 4 seconds
        isLabelVisible = false
    }

    // Wrap the whole thing in a Box so the text doesn't "push" other UI elements
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End // Keeps circle as the anchor
        ) {
            // THE SPEECH BUBBLE
            AnimatedVisibility(
                visible = isLabelVisible && displayedText.isNotEmpty(),
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Surface(
                    color = Color(0xFF0284C7),
                    // Bubble shape pointing towards the button
                    shape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 20.dp),
                    modifier = Modifier.padding(end = 8.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = displayedText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // THE MAIN BUTTON
            Surface(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}