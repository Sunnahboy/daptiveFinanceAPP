package com.finadapt.adaptivefinance.feature.expense.ocr

import android.util.Log
import com.finadapt.adaptivefinance.core.network.ApiClient
import com.finadapt.adaptivefinance.data.remote.ReceiptRequest
import com.finadapt.adaptivefinance.feature.expense.ParsedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.net.SocketTimeoutException

object ReceiptParser {

    suspend fun parseWithLlm(formattedText: String, userId: String): ParsedReceipt = withContext(Dispatchers.IO) {
        try {
            // 1. Send the messy OCR text to secure Ktor backend
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
            if (e is UnknownHostException || e is SocketTimeoutException) {
                throw Exception("Network error. Please check your connection and try again.")
            }
            // Preserve specific validation or rate limit messages
            if (e.message?.contains("valid receipt") == true || e.message?.contains("Rate limit") == true) {
                throw e
            }
            Log.e("ReceiptParser", "Failed to parse receipt via backend", e)
            throw Exception("Could not process this image as a receipt. Please enter details manually.")
        }
    }
}
