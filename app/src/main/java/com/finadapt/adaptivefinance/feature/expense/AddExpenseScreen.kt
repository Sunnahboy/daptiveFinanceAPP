package com.finadapt.adaptivefinance.feature.expense

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    currencySymbol: String,
    onLogExpense: (Float, String, String, String, String, String, List<ReceiptItem>) -> Unit,
    onDismissState: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ================= UI STATES =================
    var amountInput by remember { mutableStateOf("") }
    var merchantInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }

    // Feedback & Loading States
    var voiceFeedbackMsg by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Bottom Sheet States (Receipt OCR)
    var showReceiptSheet by remember { mutableStateOf(false) }
    var scannedReceipt by remember { mutableStateOf<ParsedReceipt?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ================= TTS INITIALIZATION =================
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

    val coroutineScope = rememberCoroutineScope()

    // ================= SCANNER LAUNCHER =================
    val scannerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)

            val uri = scanningResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                isScanning = true
                voiceFeedbackMsg = "Analyzing receipt securely..."

                coroutineScope.launch {
                    try {
                        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        val userId = prefs.getString("USER_ID", "default_user") ?: "default_user"

                        val receipt = ReceiptScanner.analyzeReceipt(context, uri, userId)
                        scannedReceipt = receipt
                        showReceiptSheet = true

                        if (receipt.items.isNotEmpty()) {
                            voiceFeedbackMsg = "Found ${receipt.items.size} items from ${receipt.merchantName}."
                            if (isTtsReady) tts?.speak("Receipt analyzed. Please review.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        } else {
                            voiceFeedbackMsg = "Digital receipt saved. Please enter details manually."
                            if (isTtsReady) tts?.speak("Receipt saved. Please enter the amount.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                        isScanning = false
                    } catch (_: Exception) {
                        voiceFeedbackMsg = "Error processing image."
                        isScanning = false
                    }
                }
            }
        }
    }

    // ================= THEME COLORS =================
    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

    // ================= VALIDATION LOGIC =================
    val parsedAmount = amountInput.toFloatOrNull()
    val isAmountMissing = parsedAmount == null || parsedAmount <= 0f
    val isCategoryMissing = categoryInput.isBlank() || categoryInput == "Other"

    val buttonLabel = when {
        isAmountMissing && isCategoryMissing -> "Enter amount and choose a category"
        isAmountMissing -> "Enter an amount"
        isCategoryMissing -> "Choose a category"
        else -> "Save $currencySymbol ${parsedAmount?.let { com.finadapt.adaptivefinance.core.util.FinanceFormatting.formatCurrency(it, "") } ?: ""}"
    }

    val buttonEnabled = !isAmountMissing && !isCategoryMissing && !isSubmitting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---------------- HEADER (6-10dp below status bar) ----------------
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Log expense",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                IconButton(onClick = onDismissState) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                }
            }

            // ---------------- SCROLLABLE CONTENT AREA ----------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ---------------- 1. AMOUNT INPUT SECTION ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("How much did you spend?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Outlined Buttons: Scan receipt and Voice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { ReceiptScanner.startScanUI(context as android.app.Activity, scannerLauncher) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(color = secondaryColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan receipt", color = textColor)
                                }
                            }

                            var isListening by remember { mutableStateOf(false) }
                            val speechRecognizer = remember {
                                android.speech.SpeechRecognizer.createSpeechRecognizer(context).apply {
                                    setRecognitionListener(object : android.speech.RecognitionListener {
                                        override fun onReadyForSpeech(params: android.os.Bundle?) {}
                                        override fun onBeginningOfSpeech() {}
                                        override fun onRmsChanged(rmsdB: Float) {}
                                        override fun onBufferReceived(buffer: ByteArray?) {}
                                        override fun onEndOfSpeech() {}
                                        override fun onError(error: Int) {
                                            isListening = false
                                            voiceFeedbackMsg = "Couldn't hear you clearly."
                                        }
                                        override fun onResults(results: android.os.Bundle?) {
                                            isListening = false
                                            val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                                            val spokenString = matches?.firstOrNull() ?: ""

                                            val parsedData = VoiceExpenseParser.parse(spokenString)
                                            if (parsedData.first != null && parsedData.first!! > 0f) {
                                                amountInput = parsedData.first.toString()
                                            }
                                            categoryInput = parsedData.second

                                            if (isTtsReady) {
                                                if (parsedData.first != null && parsedData.second != "General") {
                                                    voiceFeedbackMsg = "Logged $currencySymbol ${parsedData.first} for ${parsedData.second}."
                                                    tts?.speak("Got it. ${parsedData.first} for ${parsedData.second}.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                                } else {
                                                    voiceFeedbackMsg = "Heard: \"$spokenString\""
                                                }
                                            }
                                        }
                                        override fun onPartialResults(partialResults: android.os.Bundle?) {}
                                        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                                    })
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isListening) {
                                        speechRecognizer.stopListening()
                                        isListening = false
                                    } else {
                                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                        }
                                        speechRecognizer.startListening(intent)
                                        isListening = true
                                        voiceFeedbackMsg = "Listening..."
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                if (isListening) {
                                    CircularProgressIndicator(color = secondaryColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Voice", color = textColor)
                                }
                            }
                        }

                        if (voiceFeedbackMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = voiceFeedbackMsg,
                                color = if (voiceFeedbackMsg.contains("error", true)) errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount Field
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { newValue ->
                                val sanitized = newValue.replace(',', '.')
                                if (sanitized.isEmpty()) {
                                    amountInput = sanitized
                                } else {
                                    val filtered = sanitized.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1) {
                                        amountInput = filtered
                                    }
                                }
                            },
                            placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 32.sp, fontWeight = FontWeight.Bold) },
                            prefix = { Text("$currencySymbol ", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = secondaryColor) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = secondaryColor,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                // Inline Amount Error (Only triggers if the user types something invalid, not on blank load)
                val isAmountError = amountInput.isNotEmpty() && (parsedAmount == null || parsedAmount <= 0f)
                if (isAmountError) {
                    Text(
                        text = "Please enter a valid amount greater than 0",
                        color = errorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------------- 2. MERCHANT INPUT ----------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Merchant (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        placeholder = { Text("e.g. Starbucks, Amazon, Grab") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            autoCorrect = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = secondaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // ---------------- 3. CATEGORY SELECTION ----------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displayCategories = listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment", "Other")

                        displayCategories.forEach { category ->
                            val isSelected = if (category == "Other") {
                                categoryInput.isNotBlank() && !listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment").contains(categoryInput)
                            } else {
                                categoryInput.equals(category, ignoreCase = true)
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (category == "Other") {
                                        if (isSelected) categoryInput = "" else categoryInput = "Other"
                                    } else {
                                        categoryInput = category
                                    }
                                },
                                label = {
                                    Text(
                                        text = category,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = secondaryColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.Transparent,
                                    labelColor = textColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = secondaryColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    val isOtherSelected = categoryInput.isNotBlank() && !listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment").contains(categoryInput)

                    if (isOtherSelected) {
                        OutlinedTextField(
                            value = if (categoryInput == "Other") "" else categoryInput,
                            onValueChange = { categoryInput = it },
                            placeholder = { Text("Enter custom category") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = secondaryColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ---------------- 4. PINNED BOTTOM SAVE BAR ----------------
            Surface(
                color = bgColor,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Helper hint above the button if something is missing
                    if (!buttonEnabled) {
                        Text(
                            text = buttonLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            if (buttonEnabled) {
                                isSubmitting = true
                                keyboardController?.hide()

                                val finalMerchant = merchantInput.takeIf { it.isNotBlank() } ?: categoryInput

                                onLogExpense(
                                    amountInput.toFloat(),
                                    categoryInput,
                                    finalMerchant,
                                    scannedReceipt?.date ?: "",
                                    scannedReceipt?.paymentMethod ?: "",
                                    scannedReceipt?.localImagePath ?: "",
                                    scannedReceipt?.items ?: emptyList()
                                )

                                isSubmitting = false
                                amountInput = ""
                                merchantInput = ""
                                categoryInput = ""
                                scannedReceipt = null
                                voiceFeedbackMsg = ""

                                onDismissState()
                            }
                        },
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (buttonEnabled) secondaryColor else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (buttonEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (buttonEnabled) buttonLabel else "Save expense",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ================= BOTTOM SHEET: DIGITAL RECEIPT =================
        if (showReceiptSheet && scannedReceipt != null) {
            val receipt = scannedReceipt!!
            ModalBottomSheet(
                onDismissRequest = { showReceiptSheet = false },
                sheetState = sheetState,
                containerColor = cardColor
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(receipt.merchantName.ifEmpty { "Receipt Saved" }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = secondaryColor)
                            if (receipt.date.isNotEmpty()) { Text("${receipt.date} • ${receipt.paymentMethod}", color = Color.Gray, fontSize = 14.sp) }
                        }

                        if (receipt.localImagePath.isNotEmpty()) {
                            var showFullImage by remember { mutableStateOf(false) }
                            AsyncImage(
                                model = File(receipt.localImagePath),
                                contentDescription = "Receipt Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showFullImage = true }
                            )
                            if (showFullImage) {
                                Dialog(onDismissRequest = { showFullImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
                                        AsyncImage(model = File(receipt.localImagePath), contentDescription = "Full Receipt", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        IconButton(onClick = { showFullImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (receipt.items.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(receipt.items) { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1)
                                        Text(item.category, fontSize = 12.sp, color = secondaryColor)
                                    }
                                    Text("$currencySymbol ${String.format(Locale.US, "%.2f", item.amount)}", fontWeight = FontWeight.Bold, color = textColor)
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("No line items extracted. You can manually enter the total amount below.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total OCR Amount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("$currencySymbol ${String.format(Locale.US, "%.2f", receipt.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = secondaryColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            amountInput = receipt.total.toString()
                            if (receipt.merchantName.isNotBlank()) merchantInput = receipt.merchantName
                            showReceiptSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply to Form", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
