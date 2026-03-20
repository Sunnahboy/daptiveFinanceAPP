package com.finadapt.adaptivefinance.feature.expense

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.finadapt.adaptivefinance.BuildConfig // 🟢 Imports your generated API key!
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

// 🟢 FIX 1 & 2: Added the missing data classes right here!
data class ReceiptItem(
    val name: String,
    val amount: Float,
    val category: String
)

data class ParsedReceipt(
    val merchantName: String,
    val date: String,
    val paymentMethod: String,
    val items: List<ReceiptItem>,
    val total: Float,
    var localImagePath: String = "" // Holds the digital receipt photo path
)

object ReceiptScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // 🟢 FIX 3: Pull the API key safely from your Gradle BuildConfig
    private var OPENROUTER_API_KEY = BuildConfig.OPENROUTER_API_KEY

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) // Fail fast if the server is dead
        .readTimeout(45, TimeUnit.SECONDS)   // If the AI takes longer than 10s, kill it and try the next model!
        .build()

    private val freeModels = listOf(
        //"google/gemini-2.0-flash-lite-preview-02-05:free", // Google's fastest free model
        //"meta-llama/llama-3.1-8b-instruct:free",           // Meta's updated free model
        //"google/gemma-2-9b-it:free",                       // Google's updated open model
        //"openrouter/auto",
        //"openrouter/free"
        "mistralai/mistral-small-3.2-24b-instruct:free",
        "meta-llama/llama-3.2-3b-instruct:free",
        "google/gemma-3-4b-it:free",
        "google/gemma-3-12b-it:free"
    )
    private var currentModelIndex = 0

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
        // 1. Save locally first (Guarantees we never lose the image)
        val permanentPath = saveReceiptImageLocally(context, uri)

        return try {
            // 2. Read from the safe, permanent file
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(permanentPath)))

            // 3. Extract Text
            val visionText = recognizer.process(image).await()
            val rawText = visionText.text

            if (rawText.isBlank()) {
                return ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
            }

            // 4. Parse with AI
            val parsedData = parseWithOpenRouter(rawText)
            parsedData.localImagePath = permanentPath
            parsedData

        } catch (e: Exception) {
            // 🟢 FIX: Respect coroutine cancellation!
            if (e is CancellationException) throw e

            Log.e("ReceiptScanner", "Pipeline Failure", e)

            // Fallback: Return empty data but KEEP the image path so the UI can show the photo
            ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
        }
    }

    private suspend fun parseWithOpenRouter(rawText: String): ParsedReceipt = withContext(Dispatchers.IO) {

        // 🟢 FIX 1: The "Jedi Mind Trick" Prompt
        // We must explicitly tell the AI not to trigger its privacy filters.
        val systemPrompt = """
            You are a data extraction tool.  
            Ignore any personal information or addresses.
            
            Extract the following:
            1. "merchant_name"
            2. "date" (YYYY-MM-DD)
            3. "payment_method"
            4. "items": List of purchased items with "name", "amount", and "category" (Food, Groceries, Transport, Shopping, Entertainment, General).
            5. "total"
            
            CRITICAL: Return ONLY valid JSON. Start with { and end with }. Do not use markdown. 
            DO NOT output your thought process, reasoning, or any conversational text.
        """.trimIndent()

        var lastException: Exception? = null

        for (i in freeModels.indices) {
            val modelToTry = freeModels[(currentModelIndex + i) % freeModels.size]
            Log.d("ReceiptScanner", "Attempting OCR parse with: $modelToTry")

            try {
                val jsonPayload = JSONObject().apply {
                    put("model", modelToTry)
                    put("temperature", 0.0)
                    put("max_tokens", 2500)

                    put("messages", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                        put(JSONObject().apply { put("role", "user"); put("content", rawText) })
                    })
                }

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
                    .addHeader("HTTP-Referer", "https://adaptivefinance.com")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: throw Exception("Empty response from API")

                // 🟢 FIX 2: Deep Logging. Check your Android Studio 'Logcat' tab if it fails!
                Log.d("ReceiptScanner", "Raw API Response from $modelToTry: $responseBody")

                if (!response.isSuccessful) {
                    throw Exception("API Error ${response.code}: $responseBody")
                }

                val rootJson = JSONObject(responseBody)

                if (rootJson.has("error")) {
                    val errorMsg = rootJson.getJSONObject("error").optString("message", "Unknown API Error")
                    throw Exception("OpenRouter Error: $errorMsg")
                }

                val choiceObj = rootJson.getJSONArray("choices").getJSONObject(0)
                val messageObj = choiceObj.getJSONObject("message")

                // 🟢 FIX 3: Safely catch the 'null' refusal
                if (messageObj.isNull("content")) {
                    val finishReason = choiceObj.optString("finish_reason", "unknown")
                    throw Exception("AI Refused to answer. Finish reason: $finishReason")
                }

                val aiMessageStr = messageObj.getString("content")

                val startIndex = aiMessageStr.indexOf('{')
                val endIndex = aiMessageStr.lastIndexOf('}')

                if (startIndex == -1 || endIndex == -1) {
                    throw Exception("AI did not return valid JSON. Output: $aiMessageStr")
                }

                val cleanJsonStr = aiMessageStr.substring(startIndex, endIndex + 1)
                val resultData = JSONObject(cleanJsonStr)

                val merchant = resultData.optString("merchant_name", "Unknown Merchant")
                val date = resultData.optString("date", "Unknown Date")
                val payment = resultData.optString("payment_method", "Unknown")
                val explicitTotal = resultData.optDouble("total", 0.0).toFloat()

                val itemsArray = resultData.optJSONArray("items")
                val parsedItems = mutableListOf<ReceiptItem>()

                if (itemsArray != null) {
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        val name = itemObj.optString("name", "Unknown Item")
                        val amount = itemObj.optDouble("amount", 0.0).toFloat()
                        val category = itemObj.optString("category", "General")
                        if (amount > 0f) parsedItems.add(ReceiptItem(name, amount, category))
                    }
                }

                val finalTotal = if (explicitTotal > 0f) explicitTotal else parsedItems.sumOf { it.amount.toDouble() }.toFloat()
                currentModelIndex = (currentModelIndex + i + 1) % freeModels.size

                return@withContext ParsedReceipt(merchant, date, payment, parsedItems, finalTotal)

            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Model $modelToTry failed: ${e.message}")
                lastException = e
                continue // Try the next model
            }
        }

        throw Exception("AI Parsing failed after trying all models. Last Error: ${lastException?.message}")
    }

    private suspend fun saveReceiptImageLocally(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
        val directory = File(context.filesDir, "receipts")
        if (!directory.exists()) directory.mkdirs()

        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(directory, fileName)

        inputStream.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return@withContext destinationFile.absolutePath
    }
}