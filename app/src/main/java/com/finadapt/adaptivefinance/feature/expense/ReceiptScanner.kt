
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
import com.google.gson.annotations.SerializedName
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

data class ReceiptItem(
    @SerializedName("name") private val _name: String? = null,
    @SerializedName("amount") private val _amount: Float? = null,
    @SerializedName("category") private val _category: String? = null
) {

    val name: String get() = _name ?: "Unknown Item"
    val amount: Float get() = _amount ?: 0f
    val category: String get() = _category ?: "General"
}

data class ParsedReceipt(
    @SerializedName("merchant_name") private val _merchantName: String? = null,
    @SerializedName("date") private val _date: String? = null,
    @SerializedName("payment_method") private val _paymentMethod: String? = null,
    @SerializedName("items") private val _items: List<ReceiptItem>? = null,
    @SerializedName("total") private val _total: Float? = null,

    @Transient var localImagePath: String = ""
) {
    val merchantName: String get() = _merchantName ?: ""
    val date: String get() = _date ?: ""
    val paymentMethod: String get() = _paymentMethod ?: ""
    val items: List<ReceiptItem> get() = _items ?: emptyList()
    val total: Float get() = _total ?: 0f
}

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

    suspend fun analyzeReceipt(context: Context, uri: Uri,userId:String): ParsedReceipt {
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

            // 5. parse the data (with retries and defensive JSON)
            val parsedData = ReceiptParser.parseWithLlm(cleanText, userId)

            parsedData.localImagePath = permanentPath
            parsedData

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ReceiptScanner", "Pipeline Failure", e)
            ParsedReceipt("", "", "", emptyList(), 0f, permanentPath)
        }
    }
//from ReceiptScanner.kt
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