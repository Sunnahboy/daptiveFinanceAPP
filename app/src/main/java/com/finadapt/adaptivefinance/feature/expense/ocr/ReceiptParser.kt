package com.finadapt.adaptivefinance.feature.expense.ocr

import android.util.Log
import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.remote.ReceiptRequest
import com.finadapt.adaptivefinance.feature.expense.ParsedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReceiptParser {

    suspend fun parseWithLlm(formattedText: String, userId: String): ParsedReceipt = withContext(Dispatchers.IO) {
        try {
            // 1. Send the messy OCR text to  secure Ktor backend
            val request = ReceiptRequest(rawText = formattedText)
            val response = ApiClient.rateLimitApiService.parseReceipt(userId, request)

            // 2. Safely return the formatted data, or handle the errors
            when {
                response.isSuccessful && response.body() != null -> {
                    return@withContext response.body()!!
                }
                response.code() == 429 -> {
                    throw Exception("Rate limit exceeded. Please wait a moment before scanning again.")
                }
                else -> {
                    throw Exception("Backend failed to parse receipt (Code: ${response.code()})")
                }
            }
        } catch (e: Exception) {
            Log.e("ReceiptParser", "Failed to parse receipt via backend", e)
            throw Exception("Network error or invalid receipt format.")
        }
    }
}