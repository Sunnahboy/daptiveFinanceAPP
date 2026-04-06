package com.finadapt.adaptivefinance.feature.expense

object VoiceExpenseParser {

    /**
     * Takes raw spoken text and returns a Pair containing the Amount and the Category.
     * Example input: "I just spent 15.50 on grab"
     * Returns: Pair(15.5f, "Transport")
     */
    fun parse(spokenText: String): Pair<Float?, String> {
        // 1. EXTRACT THE AMOUNT (Bulletproof Regex)
        // Matches digits and optional decimals, plus handles commas (e.g., "1,250.50")
        val amountRegex = Regex("([\\d,]+(\\.\\d{1,2})?)")
        val amountMatch = amountRegex.find(spokenText)

        // Clean out any commas before casting to Float
        val amount = amountMatch?.value?.replace(",", "")?.toFloatOrNull()

        // 2. EXTRACT THE CATEGORY (The DRY Architecture)
        // We route the raw text straight into our master centralized brain!
        val category = CategoryDictionary.categorize(spokenText)

        return Pair(amount, category)
    }
}