package com.finadapt.adaptivefinance.feature.expense.ocr

object ReceiptValidator {
    /**
     * Validates whether text contains at least 2 of the 7 essential elements of a receipt:
     * 1. Header/Title (Receipt, Invoice, Store name)
     * 2. Date and Time
     * 3. Unique Transaction Number
     * 4. Seller Details (Tel, Tax ID, Address)
     * 5. Itemized Goods or Services
     * 6. Total Amount Paid (Subtotal, Total, prices)
     * 7. Payment Method (Cash, Card, M-Pesa, etc.)
     */
    fun isLikelyReceipt(text: String): Boolean {
        val lowercase = text.lowercase()

        // 1. Header/Title indicators
        val hasHeader = listOf("receipt", "invoice", "bill", "store", "shop", "market", "supermarket", "restaurant", "cafe", "bar", "outlet").any { lowercase.contains(it) }

        // 2. Date and Time indicators
        val hasDate = listOf("date", "time", "am", "pm", "2024", "2025", "2026").any { lowercase.contains(it) } ||
                Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""").containsMatchIn(lowercase)

        // 3. Unique Transaction Number
        val hasTransactionNo = listOf("order", "trans", "ticket", "#", "inv", "slip", "no.").any { lowercase.contains(it) }

        // 4. Seller Details
        val hasSeller = listOf("tel", "phone", "tax", "pin", "vat", "gst", "address", "box").any { lowercase.contains(it) }

        // 5. Itemized Goods or Services
        val hasItems = listOf("item", "qty", "pcs", "price", "unit", "desc").any { lowercase.contains(it) }

        // 6. Total Amount Paid
        val hasTotal = listOf("total", "subtotal", "sub total", "balance", "due", "sum", "amount").any { lowercase.contains(it) } ||
                Regex("""\d+[.,]\d{2}""").containsMatchIn(lowercase)

        // 7. Payment Method
        val hasPayment = listOf("cash", "card", "visa", "mastercard", "mpesa", "m-pesa", "airtel", "paid", "change", "auth").any { lowercase.contains(it) }

        val matchedElements = listOf(hasHeader, hasDate, hasTransactionNo, hasSeller, hasItems, hasTotal, hasPayment).count { it }

        // A valid receipt must contain at least 2 of the 7 essential elements
        return matchedElements >= 2
    }
}
