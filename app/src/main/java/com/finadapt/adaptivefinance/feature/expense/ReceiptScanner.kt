package com.finadapt.adaptivefinance.feature.expense

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

data class ReceiptItem(
    val name: String,
    val amount: Float,
    val category: String
)

object ReceiptScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val ignoreKeywords = listOf("tax", "cash", "change", "sst", "gst", "rounding", "visa", "mastercard", "balance", "tunai", "kembali", "quantity", "receipt", "sales", "pay", "card", "debit")
    private val totalKeywords = listOf("total", "net", "jumlah", "amount", "grand", "bil", "bayar")

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

    fun processScanResult(
        context: Context,
        result: GmsDocumentScanningResult?,
        onSuccess: (List<ReceiptItem>, Float) -> Unit,
        onError: (String) -> Unit
    ) {
        val uri = result?.pages?.firstOrNull()?.imageUri
        if (uri == null) { onError("No image captured."); return }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val visionText = recognizer.process(image).await()
                val elements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }

                withContext(Dispatchers.Main) {
                    if (elements.isEmpty()) {
                        onError("No text found. Ensure the receipt is well-lit.")
                        return@withContext
                    }

                    val (items, total) = parseElementsWithGeometry(elements)
                    if (items.isNotEmpty() || total > 0f) onSuccess(items, total)
                    else onError("Could not read line items clearly.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error: ${e.message}") }
            }
        }
    }

    private fun parseElementsWithGeometry(elements: List<Text.Element>): Pair<List<ReceiptItem>, Float> {
        val globalMaxX = elements.maxOfOrNull { it.boundingBox?.right ?: 0 } ?: 0
        val globalMinX = elements.minOfOrNull { it.boundingBox?.left ?: 0 } ?: 0
        val receiptWidth = (globalMaxX - globalMinX).coerceAtLeast(1)

        val priceZoneStart = globalMaxX - (receiptWidth * 0.35).toInt()

        val rows = groupElementsIntoRows(elements)
        val parsedItems = mutableListOf<ReceiptItem>()
        var explicitTotal = 0f
        var pendingName = ""

        // Allowed commas in prices (e.g., 1,234.50)
        val priceRegex = Regex("""(?:RM)?\s*([\d,]+[.,]\d{2})\s*[A-Z*]?$""", RegexOption.IGNORE_CASE)
        val barcodeRegex = Regex("""^[\d\s*.]+$""")

        for (row in rows) {
            val sortedElements = row.sortedBy { it.boundingBox?.left ?: 0 }
            val fullRowText = sortedElements.joinToString(" ") { it.text }.trim()
            val lowerRowText = fullRowText.lowercase(Locale.ROOT)

            // 1. Word Boundaries for Totals (\b)
            if (totalKeywords.any { Regex("\\b$it\\b").containsMatchIn(lowerRowText) }) {
                val totalMatch = priceRegex.find(fullRowText)
                if (totalMatch != null) {
                    // Strip commas before turning into a Float
                    val foundTotal = totalMatch.groupValues[1].replace(",", "").toFloatOrNull() ?: 0f
                    if (foundTotal >= explicitTotal) explicitTotal = foundTotal
                }
                continue
            }

            // 2. Word Boundaries for Noise Filter (\b)
            if (ignoreKeywords.any { Regex("\\b$it\\b").containsMatchIn(lowerRowText) }) continue

            // 3. PRICE DETECTION
            val priceElement = sortedElements.findLast { elem ->
                priceRegex.matches(elem.text.trim()) && (elem.boundingBox?.right ?: 0) >= priceZoneStart
            }

            if (priceElement != null) {
                val priceStr = priceRegex.find(priceElement.text.trim())?.groupValues?.get(1)?.replace(",", "") ?: continue
                val price = priceStr.toFloatOrNull() ?: continue

                val itemNamePart = sortedElements
                    .filter { it != priceElement }
                    .joinToString(" ") { it.text }
                    .trim()

                val finalItemName = if (itemNamePart.isEmpty() || barcodeRegex.matches(itemNamePart)) {
                    pendingName
                } else {
                    itemNamePart.replace(Regex("""^\d+\s+"""), "").trim()
                }

                // Log the item if it has a valid name and price
                if (finalItemName.length > 2 && price > 0f) {
                    val category = CategoryDictionary.categorize(finalItemName)
                    parsedItems.add(ReceiptItem(finalItemName.uppercase(), price, category))
                }
                pendingName = ""
            } else {
                val candidate = fullRowText.replace(Regex("""@\s*\d+[.,]\d+"""), "").trim()
                if (candidate.length > 2 && !barcodeRegex.matches(candidate)) {
                    pendingName = candidate
                }
            }
        }

        val finalTotal = if (explicitTotal > 0f) explicitTotal else parsedItems.sumOf { it.amount.toDouble() }.toFloat()
        return Pair(parsedItems, finalTotal)
    }

    private fun groupElementsIntoRows(elements: List<Text.Element>): List<List<Text.Element>> {
        val sorted = elements.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        val rows = mutableListOf<MutableList<Text.Element>>()

        for (element in sorted) {
            val centerY = element.boundingBox?.centerY() ?: 0
            val row = rows.find { existing ->
                val existingElem = existing.first()
                // Clamped the threshold to prevent giant fonts from swallowing the receipt
                val height = existingElem.boundingBox?.height()?.toFloat() ?: 20f
                val threshold = (height * 0.5f).coerceIn(12f, 35f)

                abs((existingElem.boundingBox?.centerY() ?: 0) - centerY) < threshold
            }
            if (row != null) row.add(element) else rows.add(mutableListOf(element))
        }
        return rows
    }
}