package com.finadapt.adaptivefinance.feature.expense

import com.finadapt.adaptivefinance.feature.expense.ocr.ReceiptValidator
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptValidationTest {

    @Test
    fun testIsLikelyReceiptSevenElements() {
        // Valid receipt samples (contain >= 2 of the 7 essential elements)
        val validReceipt1 = "SUPERMARKET RECEIPT\nDate: 2024-05-10\nTotal: 150.00\nCash" // Header + Date + Total + Payment = 4 elements
        val validReceipt2 = "Invoice #12345\nKSH 450.50" // TransactionNo + Total = 2 elements
        val validReceipt3 = "Restaurant Bill\nItem 1 x 500.00" // Header + Items + Total = 3 elements

        // Invalid non-receipt samples (tech specs, keyboard photos, books)
        val invalid1 = "Mechanical Keyboard RGB Backlit 108.00mm" // Just random specs, 0 or 1 element
        val invalid2 = "Chapter 1: The Beginning of the Journey into the Forest"
        val invalid3 = "Notice: Parking is prohibited between 8 AM and 5 PM."

        assertEquals(true, ReceiptValidator.isLikelyReceipt(validReceipt1))
        assertEquals(true, ReceiptValidator.isLikelyReceipt(validReceipt2))
        assertEquals(true, ReceiptValidator.isLikelyReceipt(validReceipt3))

        assertEquals(false, ReceiptValidator.isLikelyReceipt(invalid1))
        assertEquals(false, ReceiptValidator.isLikelyReceipt(invalid2))
        assertEquals(false, ReceiptValidator.isLikelyReceipt(invalid3))
    }
}
