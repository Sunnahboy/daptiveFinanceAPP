package com.finadapt.adaptivefinance.feature.expense

import java.util.Locale

object CategoryDictionary {

    val categoryMap = mapOf(
        "Food" to listOf("burger", "coffee", "mcd", "kfc", "starbucks", "cafe", "chicken", "meal", "bistro", "pizza", "nasi", "mee", "roti", "kuih", "teh", "milo", "food", "dining", "eat", "lunch", "dinner", "breakfast"),
        "Groceries" to listOf("aeon", "tesco", "walmart", "milk", "bread", "mart", "supermarket", "lotus", "daging", "cili", "carrot", "ubi", "limau", "daun", "sayur", "bawang", "ikan", "ayam", "telur", "beras", "super seven", "epal", "kisar", "groceries"),
        "Transport" to listOf("petronas", "shell", "grab", "uber", "taxi", "parking", "toll", "petrol", "minyak", "touch", "bus", "train", "gas", "fuel", "transport"),
        "Shopping" to listOf("uniqlo", "zara", "hm", "shirt", "shoes", "mall", "apparel", "watsons", "guardian", "baju", "seluar", "clothes", "shopping"),
        "Entertainment" to listOf("cinema", "gsc", "tgv", "steam", "psn", "netflix", "ticket", "wayang", "movie", "game", "entertainment")
    )

    /**
     * Universal categorizer used by both Voice and Vision engines.
     */
    fun categorize(input: String): String {
        val lowerInput = input.lowercase(Locale.ROOT)
        for ((category, keywords) in categoryMap) {
            // Using Word Boundaries (\b) so "cash" doesn't match "cashew"
            if (keywords.any { Regex("\\b$it\\b").containsMatchIn(lowerInput) }) {
                return category
            }
        }
        return "General"
    }
}