package com.finadapt.adaptivefinance.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.finadapt.adaptivefinance.data.local.ChatThreadEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val threads by viewModel.threads.collectAsState()
    val currentThreadId by viewModel.currentThreadId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isDrawerOpen by remember { mutableStateOf(false) }

    // Session-level tracker to prevent retyping on scroll
    val animatedMessageIds = remember { mutableStateMapOf<String, Boolean>() }

    // Dialog state for renaming thread
    var threadToRename by remember { mutableStateOf<ChatThreadEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    // Predefined Smart Questions
    val suggestedQuestions = listOf(
        "What did I spend today?",
        "What was my highest purchase?",
        "Where am I spending the most?",
        "What is my remaining budget?"
    )

    // Theming Colors with Glassmorphism Palette
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.background
    val surfaceGlassColor = colorScheme.surface.copy(alpha = 0.85f)
    val surfaceVariantGlass = colorScheme.surfaceVariant.copy(alpha = 0.75f)
    val textColor = colorScheme.onBackground
    val primaryColor = colorScheme.primary

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { }, // Minimalist header
                    navigationIcon = {
                        IconButton(
                            onClick = { isDrawerOpen = true }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open Chat History",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceGlassColor,
                        titleContentColor = textColor,
                        navigationIconContentColor = textColor
                    ),
                    modifier = Modifier
                        .statusBarsPadding()
                        .shadow(2.dp)
                )
            },
            containerColor = bgColor,
            modifier = Modifier.imePadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Ambient background gradient
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.TopEnd)
                        .blur(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. CHAT MESSAGES & CLAUDE-STYLE CENTERED GREETING
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(
                                                    brush = Brush.linearGradient(listOf(primaryColor, colorScheme.secondary)),
                                                    shape = CircleShape
                                                )
                                                .shadow(8.dp, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = viewModel.greetingText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = textColor,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "What's on your mind today?",
                                            fontSize = 15.sp,
                                            color = colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                animatedMessageIds = animatedMessageIds
                            )
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                                        color = surfaceVariantGlass,
                                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = primaryColor
                                            )
                                            Text(
                                                text = "Analyzing financials...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. SUGGESTION CHIPS
                    AnimatedVisibility(
                        visible = inputText.isEmpty(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestedQuestions) { question ->
                                Surface(
                                    onClick = {
                                        if (!isLoading) {
                                            viewModel.sendMessage(question)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = surfaceVariantGlass,
                                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.18f)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            text = question,
                                            color = colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. INPUT DOCK
                    Surface(
                        color = surfaceGlassColor,
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.12f)),
                        shadowElevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
                                placeholder = {
                                    Text(
                                        "Ask AI about your expenses...",
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.2f),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedContainerColor = colorScheme.surface.copy(alpha = 0.5f),
                                    unfocusedContainerColor = colorScheme.surface.copy(alpha = 0.3f)
                                ),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(primaryColor, colorScheme.secondary)
                                        ),
                                        shape = CircleShape
                                    ),
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

        // Chat History Drawer Overlay (Controlled strictly via isDrawerOpen state)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = { isDrawerOpen = false })
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .align(Alignment.CenterStart)
                        .clickable(enabled = false) {}, // consume clicks
                    color = colorScheme.surface,
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Drawer Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "Chat History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.createNewChat()
                                    isDrawerOpen = false
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.linearGradient(listOf(primaryColor, colorScheme.secondary)),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        val pinnedThreads = threads.filter { it.isPinned }
                        val recentThreads = threads.filter { !it.isPinned }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pinnedThreads.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "PINNED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )
                                }
                                items(pinnedThreads, key = { it.threadId }) { thread ->
                                    ThreadItem(
                                        thread = thread,
                                        isSelected = thread.threadId == currentThreadId,
                                        onSelect = {
                                            viewModel.selectThread(thread.threadId)
                                            isDrawerOpen = false
                                        },
                                        onPin = { viewModel.togglePinThread(thread.threadId, !thread.isPinned) },
                                        onRename = {
                                            threadToRename = thread
                                            renameInputText = thread.title
                                        },
                                        onDelete = { viewModel.deleteThread(thread.threadId) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }

                            if (recentThreads.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "RECENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )
                                }
                                items(recentThreads, key = { it.threadId }) { thread ->
                                    ThreadItem(
                                        thread = thread,
                                        isSelected = thread.threadId == currentThreadId,
                                        onSelect = {
                                            viewModel.selectThread(thread.threadId)
                                            isDrawerOpen = false
                                        },
                                        onPin = { viewModel.togglePinThread(thread.threadId, !thread.isPinned) },
                                        onRename = {
                                            threadToRename = thread
                                            renameInputText = thread.title
                                        },
                                        onDelete = { viewModel.deleteThread(thread.threadId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (threadToRename != null) {
        AlertDialog(
            onDismissRequest = { threadToRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        threadToRename?.let {
                            viewModel.renameThread(it.threadId, renameInputText)
                        }
                        threadToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThreadItem(
    thread: ChatThreadEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) primaryColor.copy(alpha = 0.15f) else colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isSelected) BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (thread.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = thread.title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onPin, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (thread.isPinned) primaryColor else colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    messageId: String,
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    animatedMessageIds: MutableMap<String, Boolean>,
    modifier: Modifier = Modifier
) {
    val alreadyAnimated = animatedMessageIds[messageId] == true
    var textToDisplay by remember(messageId) { mutableStateOf(if (alreadyAnimated) text else "") }

    LaunchedEffect(messageId) {
        if (!alreadyAnimated) {
            for (i in text.indices) {
                textToDisplay = text.substring(0, i + 1)
                kotlinx.coroutines.delay(10)
            }
            animatedMessageIds[messageId] = true
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

@Composable
fun ChatBubble(
    message: ChatMessage,
    animatedMessageIds: MutableMap<String, Boolean>
) {
    val isUser = message.isFromUser
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary

    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val bubbleColor = if (isUser) {
        colorScheme.primary
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }

    val textColor = if (isUser) colorScheme.onPrimary else colorScheme.onSurfaceVariant
    val borderStroke = if (isUser) null else BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = borderStroke,
            shadowElevation = if (isUser) 4.dp else 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier.padding(14.dp)
            ) {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    TypewriterText(
                        messageId = message.id,
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        animatedMessageIds = animatedMessageIds
                    )
                }
            }
        }
    }
}
