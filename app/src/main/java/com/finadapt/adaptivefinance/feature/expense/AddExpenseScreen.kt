package com.finadapt.adaptivefinance.feature.expense

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    isDarkMode: Boolean,
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

    val commonCategories = listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment")

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
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val primaryColor = Color(0xFF0284C7)
    val errorColor = Color(0xFFEF4444)

    // ================= VALIDATION LOGIC =================
    val parsedAmount = amountInput.toFloatOrNull()

    // Flags true if user typed something but it's invalid (e.g., "-50", "0", "abc")
    val isAmountError = amountInput.isNotEmpty() && (parsedAmount == null || parsedAmount <= 0f)

    // Overall form validity ensures the submit button only activates when safe
    val isValid = parsedAmount != null && parsedAmount > 0f && categoryInput.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense", fontWeight = FontWeight.Bold) },
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ---------------- 1. AMOUNT INPUT SECTION ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("How much did you spend?", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Voice and Camera Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoiceInputButton(
                                onResult = { spokenString ->
                                    val parsedData = VoiceExpenseParser.parse(spokenString)
                                    if (parsedData.first != null && parsedData.first!! > 0f) {
                                        amountInput = parsedData.first.toString()
                                    }
                                    categoryInput = parsedData.second

                                    if (isTtsReady) {
                                        if (parsedData.first != null && parsedData.second != "General") {
                                            voiceFeedbackMsg = "Logged RM ${parsedData.first} for ${parsedData.second}."
                                            tts?.speak("Got it. ${parsedData.first} for ${parsedData.second}.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        } else {
                                            voiceFeedbackMsg = "Heard: \"$spokenString\""
                                        }
                                    }
                                },
                                onError = { voiceFeedbackMsg = it }
                            )

                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { ReceiptScanner.startScanUI(context as android.app.Activity, scannerLauncher) },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6))
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }

                        // Voice/Scan Status Messages
                        if (voiceFeedbackMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = voiceFeedbackMsg,
                                color = if (voiceFeedbackMsg.contains("error", true)) errorColor else Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // The Core Amount Field (With Error Handling)
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { newValue ->
                                // FIX: Convert commas to dots so international keyboards don't break the app
                                val sanitized = newValue.replace(',', '.')

                                if (sanitized.isEmpty()) {
                                    amountInput = sanitized
                                } else {
                                    // Keep the strict filtering, but apply it to the sanitized string
                                    val filtered = sanitized.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1) {
                                        amountInput = filtered
                                    }
                                }
                            },
                            isError = isAmountError,
                            prefix = { Text("RM ", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = primaryColor) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = errorColor,
                                errorTextColor = errorColor
                            ),
                            // Explicit UI Feedback if the user inputs a bad value (-50, 0, etc.)
                            supportingText = {
                                if (isAmountError) {
                                    Text(
                                        text = "Please enter a valid amount greater than 0",
                                        color = errorColor,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        )
                    }
                }

                // ---------------- 2. MERCHANT INPUT ----------------
                Text("Merchant (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    placeholder = { Text("e.g. Starbucks, Amazon, Grab") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                // ---------------- 3. CATEGORY SELECTION ----------------
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    placeholder = { Text("e.g. Food, Transport, Rent") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
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
                            label = {
                                Text(
                                    text = category,
                                    color = if (isSelected) primaryColor else textColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor.copy(alpha = 0.1f),
                                selectedLabelColor = primaryColor,
                                containerColor = Color.Transparent,
                                labelColor = textColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = textColor.copy(alpha = 0.2f),
                                selectedBorderColor = primaryColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 4. SUBMIT ACTION & UX FEEDBACK ----------------

            // FIX: Dynamic helper text that explicitly tells the user EXACTLY what is missing
            if (!isValid) {
                val missingRequirement = when {
                    amountInput.isBlank() -> "Please enter an amount"
                    isAmountError -> "Please fix the invalid amount"
                    categoryInput.isBlank() -> "Please select a category to continue"
                    else -> ""
                }

                if (missingRequirement.isNotEmpty()) {
                    Text(
                        text = missingRequirement,
                        color = errorColor, // Stand out so the user sees why the button is locked
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
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

                        // Clear form
                        isSubmitting = false
                        amountInput = ""
                        merchantInput = ""
                        categoryInput = ""
                        scannedReceipt = null
                        voiceFeedbackMsg = ""

                        onDismissState()
                    }
                },
                enabled = isValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Log Expense", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                            Text(receipt.merchantName.ifEmpty { "Receipt Saved" }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
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
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (receipt.items.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 250.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(receipt.items.size) { index ->
                                val item = receipt.items[index]
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    } else {
                        Text("No specific items extracted. Please enter the amount manually.", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val dominantCategory = receipt.items.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "General"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("RM ${String.format(java.util.Locale.US, "%.2f", receipt.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (receipt.total > 0f) amountInput = receipt.total.toString()
                            if (dominantCategory != "General") categoryInput = dominantCategory
                            if (receipt.merchantName.isNotBlank()) merchantInput = receipt.merchantName

                            showReceiptSheet = false
                            voiceFeedbackMsg = "Receipt applied securely."
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (receipt.total > 0f) "Apply RM ${String.format(java.util.Locale.US, "%.2f", receipt.total)}" else "Close & Enter Manually", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}