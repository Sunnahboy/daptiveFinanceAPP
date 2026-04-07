package com.finadapt.adaptivefinance.data.remote

import com.google.gson.annotations.SerializedName
import java.util.Locale

data class AiIntentResponse(
    @SerializedName(value = "intent", alternate = ["Intent"])
    val intent: String = "GENERAL_CHAT",

    @SerializedName(value = "category", alternate = ["Category"])
    val category: String = "ALL",

    @SerializedName(value = "timeframe", alternate = ["Timeframe", "timeFrame"])
    val timeframe: String = "MONTH",

    // 🟢 Catches LLM hallucinations like directReply, reply, or response!
    @SerializedName(value = "direct_reply", alternate = ["directReply", "reply", "response", "message"])
    val directReply: String = "",

    @SerializedName(value = "search_term", alternate = ["searchTerm", "search"])
    val searchTerm: String = ""
) {
    // 🟢 Fixes SQLite case-sensitivity ("transport" becomes "Transport")
    val dbSafeCategory: String
        get() = if (category.equals("ALL", ignoreCase = true)) "ALL"
        else category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun Float.toCurrency(): String {
    return "RM ${String.format(Locale.US, "%.2f", this)}"
}