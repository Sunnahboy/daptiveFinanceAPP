package com.finadapt.adaptivefinance.feature.expense

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
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
    // 🟢 ENTERPRISE UPGRADE: Discount detection
    private val discountKeywords = listOf("discount", "diskaun", "rebat", "rebate", "saving", "promo", "voucher")

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

    // 🟢 ENTERPRISE UPGRADE: Pure 'suspend' function. No memory leaks.
    // The Compose UI will call this safely within a LaunchedEffect or ViewModel.
    suspend fun analyzeReceipt(context: Context, uri: Uri): Pair<List<ReceiptItem>, Float> {
        val image = InputImage.fromFilePath(context, uri)

        // Await the ML Kit processing
        val visionText = recognizer.process(image).await()
        val elements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }

        if (elements.isEmpty()) {
            throw Exception("No readable text found. Ensure the receipt is well-lit.")
        }

        return parseElementsWithGeometry(elements)
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

        val priceRegex = Regex("""(?:RM)?\s*([\d,]+[.,]\d{2})\s*[A-Z*]?$""", RegexOption.IGNORE_CASE)
        val barcodeRegex = Regex("""^[\d\s*.]+$""")
        // 🟢 ENTERPRISE UPGRADE: Catch multipliers like "2 x 5.00"
        val multiplierRegex = Regex("""\d+\s*[xX*@]\s*[\d,]+[.,]\d{2}""")

        for (row in rows) {
            val sortedElements = row.sortedBy { it.boundingBox?.left ?: 0 }
            val fullRowText = sortedElements.joinToString(" ") { it.text }.trim()
            val lowerRowText = fullRowText.lowercase(Locale.ROOT)

            // 1. Total Hunter
            if (totalKeywords.any { Regex("\\b$it\\b").containsMatchIn(lowerRowText) }) {
                val totalMatch = priceRegex.find(fullRowText)
                if (totalMatch != null) {
                    val foundTotal = totalMatch.groupValues[1].replace(",", "").toFloatOrNull() ?: 0f
                    if (foundTotal >= explicitTotal) explicitTotal = foundTotal
                }
                continue
            }

            // 2. 🟢 ENTERPRISE UPGRADE: Discount & Noise Filter
            if (discountKeywords.any { Regex("\\b$it\\b").containsMatchIn(lowerRowText) }) continue
            if (ignoreKeywords.any { Regex("\\b$it\\b").containsMatchIn(lowerRowText) }) continue

            // 3. Price Detection
            val priceElement = sortedElements.findLast { elem ->
                priceRegex.matches(elem.text.trim()) && (elem.boundingBox?.right ?: 0) >= priceZoneStart
            }

            if (priceElement != null) {
                val priceStr = priceRegex.find(priceElement.text.trim())?.groupValues?.get(1)?.replace(",", "") ?: continue
                val price = priceStr.toFloatOrNull() ?: continue

                // Get the text to the left
                val itemNamePart = sortedElements
                    .filter { it != priceElement }
                    .joinToString(" ") { it.text }
                    .trim()

                // Clean up multipliers and barcodes
                val cleanItemNamePart = itemNamePart
                    .replace(multiplierRegex, "")
                    .replace(Regex("""^\d+\s+"""), "")
                    .trim()

                val finalItemName = if (cleanItemNamePart.isEmpty() || barcodeRegex.matches(cleanItemNamePart)) {
                    pendingName
                } else {
                    cleanItemNamePart
                }

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

        // 🟢 ENTERPRISE UPGRADE: Strict rounding to avoid Float math bugs
        val calculatedSum = parsedItems.sumOf { it.amount.toDouble() }
        val finalTotal = if (explicitTotal > 0f) explicitTotal else calculatedSum.toFloat()

        // Round to 2 decimal places to simulate safe money math
        val roundedTotal = Math.round(finalTotal * 100) / 100f

        return Pair(parsedItems, roundedTotal)
    }

    private fun groupElementsIntoRows(elements: List<Text.Element>): List<List<Text.Element>> {
        val sorted = elements.sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        val rows = mutableListOf<MutableList<Text.Element>>()

        for (element in sorted) {
            val centerY = element.boundingBox?.centerY() ?: 0
            val row = rows.find { existing ->
                val existingElem = existing.first()
                val height = existingElem.boundingBox?.height()?.toFloat() ?: 20f
                val threshold = (height * 0.5f).coerceIn(12f, 35f)
                abs((existingElem.boundingBox?.centerY() ?: 0) - centerY) < threshold
            }
            if (row != null) row.add(element) else rows.add(mutableListOf(element))
        }
        return rows
    }
}