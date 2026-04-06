package com.finadapt.adaptivefinance.feature.export

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    enum class Timeframe { DAILY, WEEKLY, MONTHLY }

    // --- 🟢 EXCEL GENERATION (Apache POI) ---
    fun generateExcelReport(context: Context, expenses: List<ExpenseEntity>, timeframe: Timeframe): File? {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("${timeframe.name} Expenses")

            // Create Header Row
            val headerRow = sheet.createRow(0)
            val headers = listOf("Date", "Category", "Merchant", "Amount (RM)", "Items")
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
            }

            // Populate Data
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            var totalAmount = 0f

            expenses.forEachIndexed { index, expense ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(dateFormatter.format(Date(expense.timestamp)))
                row.createCell(1).setCellValue(expense.category)
                row.createCell(2).setCellValue(expense.merchantName.ifEmpty { "N/A" })
                row.createCell(3).setCellValue(expense.amount.toDouble())

                val itemsStr = expense.items.joinToString(", ") { "${it.name} (RM ${it.amount})" }
                row.createCell(4).setCellValue(itemsStr.ifEmpty { "Manual Entry" })

                totalAmount += expense.amount
            }

            // Total Row
            val totalRow = sheet.createRow(expenses.size + 2)
            totalRow.createCell(2).setCellValue("TOTAL:")
            totalRow.createCell(3).setCellValue(totalAmount.toDouble())

            // Save to Cache Directory
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "AdaptiveFinance_${timeframe.name}_Report.xlsx")
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            workbook.close()
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- 🟢 PDF GENERATION (With Native Chart!) ---
    @SuppressLint("DefaultLocale")
    fun generatePdfReport(context: Context, expenses: List<ExpenseEntity>, timeframe: Timeframe): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            var currentY = 50f

            // Title
            paint.textSize = 24f
            paint.isFakeBoldText = true
            paint.color = Color.rgb(2, 132, 199) // Adaptive Blue
            canvas.drawText("Adaptive Finance - ${timeframe.name} Report", 50f, currentY, paint)
            currentY += 40f

            // Total Spend
            val totalSpend = expenses.sumOf { it.amount.toDouble() }.toFloat()
            paint.textSize = 16f
            paint.isFakeBoldText = false
            paint.color = Color.BLACK
            canvas.drawText("Total Spent: RM ${String.format("%.2f", totalSpend)}", 50f, currentY, paint)
            currentY += 60f

            // --- DRAW A SIMPLE BAR CHART ---
            val categoryTotals = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { exp -> exp.amount.toDouble() }.toFloat() }
                .toList().sortedByDescending { it.second }

            if (categoryTotals.isNotEmpty()) {
                canvas.drawText("Spending Analytics:", 50f, currentY, paint)
                currentY += 30f

                val maxAmount = categoryTotals.maxOf { it.second }
                val chartWidth = 300f

                categoryTotals.take(5).forEach { (category, amount) ->
                    // Draw Label
                    paint.color = Color.DKGRAY
                    canvas.drawText(category, 50f, currentY + 15f, paint)

                    // Draw Bar
                    paint.color = Color.rgb(2, 132, 199)
                    val barWidth = (amount / maxAmount) * chartWidth
                    canvas.drawRect(150f, currentY, 150f + barWidth, currentY + 20f, paint)

                    // Draw Amount
                    paint.color = Color.BLACK
                    canvas.drawText("RM ${amount.toInt()}", 160f + barWidth, currentY + 15f, paint)

                    currentY += 35f
                }
            }

            document.finishPage(page)

            // Save File
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, "AdaptiveFinance_${timeframe.name}_Report.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- 🟢 SHARE INTENT FIREWALL ---
    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My Adaptive Finance Report")
            putExtra(Intent.EXTRA_TEXT, "Here is my latest spending report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Report Via..."))
    }
}
