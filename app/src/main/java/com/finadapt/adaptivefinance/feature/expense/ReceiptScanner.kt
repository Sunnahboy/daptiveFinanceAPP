//package com.finadapt.adaptivefinance.feature.expense
//
//import android.app.Activity
//import android.content.Context
//import android.net.Uri
//import android.util.Log
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.IntentSenderRequest
//import com.finadapt.adaptivefinance.BuildConfig // 🟢 Imports your generated API key!
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
//import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
//import com.google.mlkit.vision.text.Text
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.File
//import java.util.concurrent.TimeUnit
//import kotlin.coroutines.cancellation.CancellationException
//import kotlin.math.abs
//
//
//data class ReceiptItem(
//    val name: String,
//    val amount: Float,
//    val category: String
//)
//
//data class ParsedReceipt(
//    val merchantName: String,
//    val date: String,
//    val paymentMethod: String,
//    val items: List<ReceiptItem>,
//    val total: Float,
//    var localImagePath: String = "" //Holds the digital receipt photo path
//)
//
//object ReceiptScanner {
//
//    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//    private const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
//
//
//    private val httpClient = OkHttpClient.Builder()
//        .connectTimeout(10, TimeUnit.SECONDS) // Fail fast if the server is dead
//        .readTimeout(45, TimeUnit.SECONDS)   // If the AI takes longer than 10s, kill it and try the next model!
//        .build()
//
//    //Round-Robin fallback!
//    private val groqModels = listOf(
//        "llama-3.3-70b-versatile",
//        "llama-3.1-8b-instant",
//        "mixtral-8x7b-32768",
//        "gemma2-9b-it"
//    )
//    private var currentModelIndex = 0
//
//
//    fun startScanUI(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
//        val options = GmsDocumentScannerOptions.Builder()
//            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
//            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
//            .setPageLimit(1)
//            .setGalleryImportAllowed(true)
//            .build()
//
//        GmsDocumentScanning.getClient(options).getStartScanIntent(activity).addOnSuccessListener { intentSender ->
//            launcher.launch(IntentSenderRequest.Builder(intentSender).build())
//        }
//    }
//
//
//
//    suspend fun analyzeReceipt(context: Context, uri: Uri): ParsedReceipt {
//        val permanentPath = saveReceiptImageLocally(context, uri)
//
//        return try {
//            val image = InputImage.fromFilePath(context, uri)
//            //.await() bridge google's tasks to coroutines
//            val visionText = recognizer.process(image).await()
//            //Reconstruct the physical rows!
//            val elements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }
//
//            if (elements.isEmpty()) {
//                return ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
//            }
//
//            //1 Filter out useless text with no cordinats
//            val sorted = elements.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
//            val rows = mutableListOf<MutableList<Text.Element>>()
//
//            //2 Group elements into physical rows using Y-axis geometry
//            for (element in sorted) {
//                val centerY = element.boundingBox?.centerY() ?: 0
//                val row = rows.find { existing ->
//                    val existingElem = existing.first()
//                    //safe-calls with fallback defaults
//                    val height = existingElem.boundingBox?.height()?.toFloat() ?: 20f
//                    val threshold = (height * 0.5f).coerceIn(12f, 35f)
//                    abs((existingElem.boundingBox?.centerY() ?: 0) - centerY) < threshold
//                }
//                if (row != null) row.add(element) else rows.add(mutableListOf(element))
//            }
//
//            //3 Construct line-by-line string
//            val spatiallyFormattedText = rows.joinToString("\n") { row ->
//                //Sort left-to-right within the row so price is always at the end
//                row.sortedBy { it.boundingBox?.left ?: 0 }.joinToString(" ") { it.text }
//            }
//
//            Log.d("ReceiptScanner", "Spatially Formatted Text sent to LLM:\n$spatiallyFormattedText")
//
//            //4 formatted text
//            val parsedData = parseWithGroq(spatiallyFormattedText)
//            parsedData.localImagePath = permanentPath
//            parsedData
//
//        } catch (e: Exception) {
//            if (e is CancellationException) throw e
//            Log.e("ReceiptScanner", "Pipeline Failure", e)
//            ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
//        }
//    }
//
//    private suspend fun parseWithGroq(rawText: String): ParsedReceipt = withContext(Dispatchers.IO) {
//
//        // 🟢 UPGRADE 1: The "Extractive Grounding" Prompt with strict Schema
//        val systemPrompt = """
//            You are a strict data extraction tool. You operate in "Extractive Mode" only.
//
//            RULES:
//            1. NEVER guess, infer, or hallucinate prices. If an item has no clear price next to or below it, skip it.
//            2. RECEIPTS WITH WEIGHTS: Supermarket receipts often show Unit Price (e.g., "@19.99") and Weight/Quantity (e.g., "*0.104"). You MUST extract the FINAL calculated price on the far right as the "amount" (e.g., 1.14).
//            3. Clean the "name" by removing barcodes, weights, and the "@" unit price. (e.g., "CILI EPAL HIJAU @10.96" becomes exactly "CILI EPAL HIJAU").
//            4. Ignore tax, change, cash, rounding adjustments, and subtotal lines.
//            5. Categorize each item strictly into one of: [Food, Groceries, Transport, Shopping, Entertainment, General].
//
//            You MUST return a JSON object matching this exact schema:
//            {
//              "merchant_name": "string or empty",
//              "date": "YYYY-MM-DD or empty",
//              "payment_method": "string or empty",
//              "items": [
//                {
//                  "name": "Cleaned item name",
//                  "amount": 0.00,
//                  "category": "String"
//                }
//              ],
//              "total": 0.00
//            }
//        """.trimIndent()
//
//        var lastException: Exception? = null
//
//        for (i in groqModels.indices) {
//            val modelToTry = groqModels[(currentModelIndex + i) % groqModels.size]
//            Log.d("ReceiptScanner", "Attempting OCR parse with Groq: $modelToTry")
//
//            try {
//                val jsonPayload = JSONObject().apply {
//                    put("model", modelToTry)
//                    put("temperature", 0.0) // 0.0 is critical for zero hallucinations
//                    put("max_tokens", 1500)
//
//                    // 🟢 UPGRADE 2: Hardware-level JSON enforcement
//                    put("response_format", JSONObject().apply { put("type", "json_object") })
//
//                    put("messages", org.json.JSONArray().apply {
//                        put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
//                        put(JSONObject().apply { put("role", "user"); put("content", "RAW RECEIPT TEXT:\n$rawText") })
//                    })
//                }
//
//                val request = Request.Builder()
//                    .url("https://api.groq.com/openai/v1/chat/completions")
//                    .addHeader("Authorization", "Bearer $GROQ_API_KEY")
//                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
//                    .build()
//
//                val response = httpClient.newCall(request).execute()
//                val responseBody = response.body?.string() ?: throw Exception("Empty response from Groq API")
//
//                if (!response.isSuccessful) {
//                    throw Exception("API Error ${response.code}: $responseBody")
//                }
//
//                val rootJson = JSONObject(responseBody)
//
//                if (rootJson.has("error")) {
//                    val errorMsg = rootJson.getJSONObject("error").optString("message", "Unknown API Error")
//                    throw Exception("Groq Error: $errorMsg")
//                }
//
//                // 🟢 UPGRADE 3: Clean, direct parsing because JSON mode guarantees structure
//                val aiMessageStr = rootJson.getJSONArray("choices")
//                    .getJSONObject(0)
//                    .getJSONObject("message")
//                    .getString("content")
//
//                val resultData = JSONObject(aiMessageStr)
//
//                val merchant = resultData.optString("merchant_name", "Unknown Merchant")
//                val date = resultData.optString("date", "Unknown Date")
//                val payment = resultData.optString("payment_method", "Unknown")
//                val explicitTotal = resultData.optDouble("total", 0.0).toFloat()
//
//                val itemsArray = resultData.optJSONArray("items")
//                val parsedItems = mutableListOf<ReceiptItem>()
//
//                if (itemsArray != null) {
//                    for (j in 0 until itemsArray.length()) {
//                        val itemObj = itemsArray.getJSONObject(j)
//                        val name = itemObj.optString("name", "Unknown Item")
//                        val amount = itemObj.optDouble("amount", 0.0).toFloat()
//                        val category = itemObj.optString("category", "General")
//
//                        // Failsafe: Don't add items with 0.0 amounts (often hallucinated noise)
//                        if (amount > 0f && name.isNotBlank() && name != "Unknown Item") {
//                            parsedItems.add(ReceiptItem(name, amount, category))
//                        }
//                    }
//                }
//
//                val finalTotal = if (explicitTotal > 0f) explicitTotal else parsedItems.sumOf { it.amount.toDouble() }.toFloat()
//
//                // Advance the model index on success so we distribute load
//                currentModelIndex = (currentModelIndex + i + 1) % groqModels.size
//
//                return@withContext ParsedReceipt(merchant, date, payment, parsedItems, finalTotal)
//
//            } catch (e: Exception) {
//                Log.e("ReceiptScanner", "Model $modelToTry failed: ${e.message}")
//                lastException = e
//                continue // Loop to the next Groq model
//            }
//        }
//
//        throw Exception("Groq Parsing failed after trying all models. Last Error: ${lastException?.message}")
//    }
//
//    private suspend fun saveReceiptImageLocally(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
//        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
//        val directory = File(context.filesDir, "receipts")
//        if (!directory.exists()) directory.mkdirs()
//
//        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
//        val destinationFile = File(directory, fileName)
//
//        inputStream.use { input ->
//            destinationFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//        return@withContext destinationFile.absolutePath
//    }
//}





package com.finadapt.adaptivefinance.feature.expense

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.finadapt.adaptivefinance.feature.expense.ocr.OcrEngine
import com.finadapt.adaptivefinance.feature.expense.ocr.ReceiptLayoutAnalyzer
import com.finadapt.adaptivefinance.feature.expense.ocr.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

data class ReceiptItem(val name: String, val amount: Float, val category: String)
data class ParsedReceipt(
    val merchantName: String,
    val date: String,
    val paymentMethod: String,
    val items: List<ReceiptItem>,
    val total: Float,
    var localImagePath: String = ""
)

object ReceiptScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun startScanUI(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setPageLimit(1)
            .setGalleryImportAllowed(true)
            .build()

        GmsDocumentScanning.getClient(options).getStartScanIntent(activity).addOnSuccessListener { intentSender ->
            launcher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    suspend fun analyzeReceipt(context: Context, uri: Uri): ParsedReceipt {
        val permanentPath = saveReceiptImageLocally(context, uri)

        return try {
            // 1. Run ML Kit Vision
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()

            // 2. Extract Valid Geometry (No !! or ?: 0 allowed)
            val elements = OcrEngine.extractValidElements(visionText)
            if (elements.isEmpty()) return ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)

            // 3. Deskew and Cluster into physical rows
            val rows = ReceiptLayoutAnalyzer.buildRows(elements)

            // 4. Format and clean string for LLM (Fixing fragmented decimals)
            val cleanText = ReceiptLayoutAnalyzer.formatForParsing(rows)
            Log.d("ReceiptScanner", "Cleaned Text sent to LLM:\n$cleanText")

            // 5. Ask Groq to parse the data (with retries and defensive JSON)
            val parsedData = ReceiptParser.parseWithGroq(cleanText)

            parsedData.localImagePath = permanentPath
            parsedData

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ReceiptScanner", "Pipeline Failure", e)
            ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
        }
    }

    private suspend fun saveReceiptImageLocally(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
        val directory = File(context.filesDir, "receipts")
        if (!directory.exists()) directory.mkdirs()

        val destinationFile = File(directory, "receipt_${System.currentTimeMillis()}.jpg")

        inputStream.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return@withContext destinationFile.absolutePath
    }
}