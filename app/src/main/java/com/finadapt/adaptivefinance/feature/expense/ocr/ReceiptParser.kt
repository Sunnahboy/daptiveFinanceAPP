package com.finadapt.adaptivefinance.feature.expense.ocr

import android.util.Log
import com.finadapt.adaptivefinance.BuildConfig
import com.finadapt.adaptivefinance.feature.expense.ParsedReceipt
import com.finadapt.adaptivefinance.feature.expense.ReceiptItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ReceiptParser {

    private const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY

    //Shorter timeouts per request so a dead model doesn't block the pipeline
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val groqModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )
    private var currentModelIndex = 0

    suspend fun parseWithGroq(formattedText: String): ParsedReceipt = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are a strict data extraction tool. You operate in "Extractive Mode" only.
            
            RULES:
            1. NEVER guess or infer prices. If an item has no clear price, skip it.
            2. Extract the FINAL calculated price on the far right as the "amount".
            3. Clean the "name" by removing barcodes and weights.
            4. Ignore tax, change, cash, and subtotal lines.
            5. Categorize each item into: [Food, Groceries, Transport, Shopping, Entertainment, General].
            
            Return JSON matching this exact schema:
            {
              "merchant_name": "string",
              "date": "YYYY-MM-DD",
              "payment_method": "string",
              "items": [
                { "name": "string", "amount": 0.00, "category": "string" }
              ],
              "total": 0.00
            }
        """.trimIndent()

        var lastException: Exception? = null

        for (i in groqModels.indices) {
            val modelToTry = groqModels[(currentModelIndex + i) % groqModels.size]

            //EXPONENTIAL BACKOFF: Wait longer between retries for rate-limiting
            if (i > 0) {
                val backoffTime = 500L * i
                Log.w("ReceiptParser", "Retrying in ${backoffTime}ms with $modelToTry...")
                delay(backoffTime)
            }

            try {
                val jsonPayload = JSONObject().apply {
                    put("model", modelToTry)
                    put("temperature", 0.0)
                    put("max_tokens", 1000)
                    put("response_format", JSONObject().apply { put("type", "json_object") })
                    put("messages", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                        put(JSONObject().apply { put("role", "user"); put("content", "RECEIPT TEXT:\n$formattedText") })
                    })
                }

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: throw Exception("Empty response")

                if (!response.isSuccessful) throw Exception("API Error ${response.code}: $responseBody")

                val rootJson = JSONObject(responseBody)

                //DEFENSIVE JSON PARSING
                val choicesArray = rootJson.optJSONArray("choices") ?: throw Exception("Missing 'choices' in JSON")
                val firstChoice = choicesArray.optJSONObject(0) ?: throw Exception("Empty 'choices' array")
                val messageObj = firstChoice.optJSONObject("message") ?: throw Exception("Missing 'message' object")
                val aiMessageStr = messageObj.optString("content", "{}")

                val resultData = JSONObject(aiMessageStr)

                val merchant = resultData.optString("merchant_name", "Unknown Merchant")
                val date = resultData.optString("date", "")
                val payment = resultData.optString("payment_method", "")
                val explicitTotal = resultData.optDouble("total", 0.0).toFloat()

                val parsedItems = mutableListOf<ReceiptItem>()

                resultData.optJSONArray("items")?.let { itemsArray ->
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.optJSONObject(j) ?: continue
                        val name = itemObj.optString("name", "Unknown Item")
                        val amount = itemObj.optDouble("amount", 0.0).toFloat()
                        val category = itemObj.optString("category", "General")

                        if (amount > 0f && name.isNotBlank() && name != "Unknown Item") {
                            parsedItems.add(ReceiptItem(name, amount, category))
                        }
                    }
                }

                val finalTotal = if (explicitTotal > 0f) explicitTotal else parsedItems.sumOf { it.amount.toDouble() }.toFloat()

                // Advance index on success
                currentModelIndex = (currentModelIndex + i + 1) % groqModels.size

                return@withContext ParsedReceipt(merchant, date, payment, parsedItems, finalTotal)

            } catch (e: Exception) {
                Log.e("ReceiptParser", "Model $modelToTry failed: ${e.message}")
                lastException = e
            }
        }

        throw Exception("All Groq models failed. Last Error: ${lastException?.message}")
    }
}