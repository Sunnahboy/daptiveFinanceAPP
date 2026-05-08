package com.finadapt.adaptivefinance.feature.expense.ocr

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ReceiptLayoutAnalyzer {
    /**
     * Reconstructs physical rows using O(n log n) clustering on deskewed coordinates.
     */
    fun buildRows(elements: List<OcrElement>): List<List<OcrElement>> {
        if (elements.isEmpty()) return emptyList()

        //1. DESKEW: Find median angle to ignore outliers (logos, vertical text)
        val sortedAngles = elements.map { it.angle }.sorted()
        val medianAngle = sortedAngles[sortedAngles.size / 2]

        //2. Project Y coordinates into a flattened space using a rotation matrix
        val cosA = cos(-medianAngle)
        val sinA = sin(-medianAngle)

        val projectedElements = elements.map { el ->
            val projectedY = el.centerX * sinA + el.centerY * cosA
            Pair(el, projectedY)
        }

        //3. CLUSTERING PREP: Sort by the new projected Y axis -> O(n log n)
        val sortedByY = projectedElements.sortedBy { it.second }

        //4. BUCKETING (1D DBSCAN-lite): Single pass clustering -> O(n)
        val rows = mutableListOf<MutableList<OcrElement>>()
        var currentRow = mutableListOf<OcrElement>()
        var currentRowAvgY = sortedByY.first().second

        for (item in sortedByY) {
            val element = item.first
            val projY = item.second

            //Dynamic threshold based on the specific element's font height
            val threshold = element.height * 0.4f

            if (currentRow.isEmpty() || abs(projY - currentRowAvgY) < threshold) {
                //Belongs to the current row
                currentRow.add(element)
                //Update running average Y for the row
                currentRowAvgY = ((currentRowAvgY * (currentRow.size - 1)) + projY) / currentRow.size
            } else {
                //Break to a new row
                rows.add(currentRow)
                currentRow = mutableListOf(element)
                currentRowAvgY = projY
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)
        //5 Sort each row left-to-right (X-axis) and return
        return rows.map { row -> row.sortedBy { it.centerX } }
    }

    /**
     * Fixes fragmented OCR tokens ["12", ".", "50"] -> ["12.50"])
     * BEFORE sending it to the LLM to reduce hallucination risk.
     */
    fun formatForParsing(rows: List<List<OcrElement>>): String {
        return rows.joinToString("\n") { row ->
            val rawLine = row.joinToString(" ") { it.text }
            //Clean up split decimals: "12 . 50" -> "12.50"
            rawLine.replace(Regex("""(\d+)\s*\.\s*(\d{2})"""), "$1.$2")
        }
    }
}