package com.finadapt.adaptivefinance.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.feature.gamification.GamificationDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    uiState: GamificationUiState,
    onLogExpense: (Float, String) -> Unit,
    onFeedback: (String, String, Boolean) -> Unit,
    onDismissState: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 🟢 1. UI INPUT STATES
    var amountInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var voiceFeedbackMsg by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    // 🟢 2. BOTTOM SHEET STATES (For Receipt Review)
    var showReceiptSheet by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf<List<ReceiptItem>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Using our centralized dictionary for the quick-chips!
    val commonCategories = listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment")

    // 🟢 3. TTS (VOICE) ENGINE LIFECYCLE
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val coroutineScope = rememberCoroutineScope() // Add this at the top of your screen!

    val scannerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)

            val uri = scanningResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                isScanning = true
                voiceFeedbackMsg = "Analyzing receipt..."

                // 🟢 Launch safely in the Compose scope!
                coroutineScope.launch {
                    try {
                        val (parsedItems, maxPrice) = ReceiptScanner.analyzeReceipt(context, uri)

                        scannedItems = parsedItems
                        showReceiptSheet = true
                        voiceFeedbackMsg = "Found ${parsedItems.size} items."
                        isScanning = false

                        if (isTtsReady) {
                            tts?.speak("I found ${parsedItems.size} items. Please review the receipt.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    } catch (e: Exception) {
                        voiceFeedbackMsg = e.message ?: "OCR Error"
                        isScanning = false
                    }
                }
            }
        }
    }

    // Colors
    val bgColor = Color(0xFFF8FAFC)
    val cardColor = Color.White
    val primaryColor = Color(0xFF0284C7)

    val isValid = amountInput.isNotBlank() && categoryInput.isNotBlank() && amountInput.toFloatOrNull() != null
    val isLoading = uiState is GamificationUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismissState) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            // --- SCROLLABLE MAIN CONTENT ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. AMOUNT INPUT & MULTIMODAL CONTROLS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("How much did you spend?", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- MULTIMODAL BUTTONS (VOICE & VISION) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // VOICE BUTTON
                            VoiceInputButton(
                                onResult = { spokenString ->
                                    val parsedData = VoiceExpenseParser.parse(spokenString)
                                    val parsedAmount = parsedData.first
                                    val parsedCategory = parsedData.second

                                    if (parsedAmount != null) amountInput = parsedAmount.toString()
                                    if (parsedCategory != null) categoryInput = parsedCategory

                                    if (isTtsReady) {
                                        if (parsedAmount != null && parsedCategory != null && parsedCategory != "General") {
                                            voiceFeedbackMsg = "Logged RM $parsedAmount for $parsedCategory."
                                            tts?.speak("Got it. $parsedAmount Ringgit for $parsedCategory.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        } else if (parsedAmount != null) {
                                            voiceFeedbackMsg = "Amount heard: RM $parsedAmount. Select a category."
                                            tts?.speak("I got $parsedAmount Ringgit, what category is that?", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        } else {
                                            voiceFeedbackMsg = "Couldn't find an amount in: \"$spokenString\""
                                            tts?.speak("Sorry, I didn't catch the amount.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        }
                                    } else {
                                        voiceFeedbackMsg = "Heard: \"$spokenString\""
                                    }
                                },
                                onError = { errorMsg ->
                                    voiceFeedbackMsg = errorMsg
                                    if (isTtsReady) tts?.speak("Microphone error.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            )

                            // SCANNER BUTTON
                            Box(
                                modifier = Modifier.size(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        // Trigger the Industrial Receipt Scanner!
                                        ReceiptScanner.startScanUI(context as android.app.Activity, scannerLauncher)
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6)) // Purple Vision Button
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.DocumentScanner,
                                            contentDescription = "Scan Receipt",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // FEEDBACK TEXT
                        if (voiceFeedbackMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = voiceFeedbackMsg,
                                color = if (voiceFeedbackMsg.contains("error", ignoreCase = true) || voiceFeedbackMsg.contains("couldn't find", ignoreCase = true)) Color.Red else Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // AMOUNT TEXT FIELD
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            prefix = { Text("RM ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = primaryColor) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            )
                        )
                    }
                }

                // 2. CATEGORY SELECTION
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    placeholder = { Text("e.g. Starbucks, Uber, Rent") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonCategories.forEach { category ->
                        val isSelected = categoryInput.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { categoryInput = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.1f),
                                selectedLabelColor = primaryColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                if (uiState is GamificationUiState.Error) {
                    Text(text = uiState.exception, color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            // --- FIXED BOTTOM AREA ---
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isValid) {
                        keyboardController?.hide()
                        onLogExpense(amountInput.toFloat(), categoryInput)
                    }
                },
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(30.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Calculating...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("Log Expense", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // --- 🟢 GAMIFICATION DIALOG ---
        if (uiState is GamificationUiState.Success) {
            if (uiState.action == "Log_Only") {
                LaunchedEffect(Unit) { onDismissState() }
            } else {
                GamificationDialog(
                    action = uiState.action,
                    message = uiState.message,
                    predictionId = uiState.predictionId,
                    onFeedback = { predId, rewardInt ->
                        onFeedback(predId, uiState.action, rewardInt == 1)
                    },
                    onDismiss = { onDismissState() }
                )
            }
        }

        // --- 🟢 REVIEW RECEIPT BOTTOM SHEET ---
        if (showReceiptSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReceiptSheet = false },
                sheetState = sheetState,
                containerColor = cardColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text("Review Receipt", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Here is what the AI extracted. You can safely discard this if it's incorrect.", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Itemized List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scannedItems.size) { index ->
                            val item = scannedItems[index]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), maxLines = 1)
                                    Text(item.category, fontSize = 12.sp, color = primaryColor)
                                }
                                Text("RM ${String.format(java.util.Locale.US, "%.2f", item.amount)}", fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Calculate the True Total and Dominant Category
                    val calculatedTotal = scannedItems.sumOf { it.amount.toDouble() }.toFloat()

                    // Finds the most frequently occurring category in the list (e.g., if 5 items are "Groceries" and 1 is "Food", it picks "Groceries")
                    val dominantCategory = scannedItems
                        .groupingBy { it.category }
                        .eachCount()
                        .maxByOrNull { it.value }?.key ?: "General"

                    // 3. Footer Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calculated Total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("RM ${String.format(java.util.Locale.US, "%.2f", calculatedTotal)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Confirm Button
                    Button(
                        onClick = {
                            // Apply parsed data to the main screen text fields!
                            amountInput = calculatedTotal.toString()
                            categoryInput = dominantCategory
                            showReceiptSheet = false
                            voiceFeedbackMsg = "Receipt applied successfully."
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply RM ${String.format(java.util.Locale.US, "%.2f", calculatedTotal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}