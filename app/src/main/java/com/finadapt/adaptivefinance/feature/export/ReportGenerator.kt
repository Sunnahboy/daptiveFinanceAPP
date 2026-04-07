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
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    enum class Timeframe { DAILY, WEEKLY, MONTHLY }

    // --- 🟢 HELPER: Map Categories to Native Canvas Colors ---
    private fun getNativeCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "food", "dining", "groceries" -> Color.rgb(239, 68, 68) // Red
            "transport", "gas", "parking" -> Color.rgb(59, 130, 246) // Blue
            "entertainment", "movies", "games" -> Color.rgb(139, 92, 246) // Purple
            "shopping", "clothes", "accessories" -> Color.rgb(245, 158, 11) // Orange
            "utilities", "bills", "rent" -> Color.rgb(16, 185, 129) // Green
            "health", "medical", "fitness" -> Color.rgb(244, 114, 182) // Pink
            else -> Color.rgb(6, 182, 212) // Cyan
        }
    }

    // --- 🟢 PREMIUM EXCEL GENERATION ---
    fun generateExcelReport(context: Context, expenses: List<ExpenseEntity>, timeframe: Timeframe): File? {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("${timeframe.name} Summary")

            // 1. Create Premium Styles
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.DARK_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(workbook.createFont().apply {
                    color = IndexedColors.WHITE.index
                    bold = true
                    fontHeightInPoints = 12
                })
            }
            val boldStyle = workbook.createCellStyle().apply {
                setFont(workbook.createFont().apply { bold = true })
            }
            val currencyStyle = workbook.createCellStyle().apply {
                dataFormat = workbook.createDataFormat().getFormat("\"RM\" #,##0.00")
            }
            val currencyBoldStyle = workbook.createCellStyle().apply {
                dataFormat = workbook.createDataFormat().getFormat("\"RM\" #,##0.00")
                setFont(workbook.createFont().apply { bold = true })
            }

            // 2. Executive Summary Section
            val totalAmount = expenses.sumOf { it.amount.toDouble() }

            val titleRow = sheet.createRow(0)
            titleRow.createCell(0).apply { setCellValue("Adaptive Finance - ${timeframe.name} Report"); cellStyle = boldStyle }

            val generatedRow = sheet.createRow(1)
            generatedRow.createCell(0).setCellValue("Generated On:")
            generatedRow.createCell(1).setCellValue(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))

            val totalSummaryRow = sheet.createRow(2)
            totalSummaryRow.createCell(0).apply { setCellValue("TOTAL OUTFLOW:"); cellStyle = boldStyle }
            totalSummaryRow.createCell(1).apply { setCellValue(totalAmount); cellStyle = currencyBoldStyle }

            // 3. Table Headers (Starts at Row 5)
            val headerRow = sheet.createRow(4)
            val headers = listOf("Date", "Category", "Merchant", "Amount", "Itemized Breakdown")
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }

            // 4. Populate Transaction Data
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
            expenses.sortedByDescending { it.timestamp }.forEachIndexed { index, expense ->
                val row = sheet.createRow(index + 5)
                row.createCell(0).setCellValue(dateFormatter.format(Date(expense.timestamp)))
                row.createCell(1).setCellValue(expense.category)
                row.createCell(2).setCellValue(expense.merchantName.ifEmpty { "Manual Entry" })

                // Formatted Currency Cell
                val amountCell = row.createCell(3)
                amountCell.setCellValue(expense.amount.toDouble())
                amountCell.cellStyle = currencyStyle

                val itemsStr = expense.items.joinToString(", ") { "${it.name} (RM ${it.amount})" }
                row.createCell(4).setCellValue(itemsStr.ifEmpty { "-" })
            }

            // 5. Manually size columns & Freeze Header Pane
            sheet.setColumnWidth(0, 20 * 256) // Date (20 characters wide)
            sheet.setColumnWidth(1, 18 * 256) // Category
            sheet.setColumnWidth(2, 25 * 256) // Merchant
            sheet.setColumnWidth(3, 15 * 256) // Amount
            sheet.setColumnWidth(4, 40 * 256) // Itemized Breakdown

            sheet.createFreezePane(0, 5) // Freezes everything above the table headers when scrolling!

            // 6. Save File
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

    // --- 🟢 ULTIMATE PDF GENERATION (Pie Chart + Bar Graph + Table) ---
    @SuppressLint("DefaultLocale")
    fun generatePdfReport(context: Context, expenses: List<ExpenseEntity>, timeframe: Timeframe): File? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }

            val totalSpend = expenses.sumOf { it.amount.toDouble() }.toFloat()

            // 1. 🟦 DARK PREMIUM HEADER
            paint.color = Color.rgb(15, 23, 42)
            canvas.drawRect(0f, 0f, 595f, 120f, paint)

            paint.color = Color.WHITE
            paint.textSize = 32f
            paint.isFakeBoldText = true
            canvas.drawText("Adaptive Finance", 40f, 60f, paint)

            paint.textSize = 16f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(148, 163, 184)
            canvas.drawText("${timeframe.name} SPENDING REPORT", 40f, 90f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("RM ${String.format("%.2f", totalSpend)}", 555f, 75f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            paint.color = Color.rgb(148, 163, 184)
            canvas.drawText("Total Outflow", 555f, 95f, paint)
            paint.textAlign = Paint.Align.LEFT

            val categoryTotals = expenses.groupBy { it.category }
                .mapValues { it.value.sumOf { exp -> exp.amount.toDouble() }.toFloat() }
                .toList().sortedByDescending { it.second }

            var currentY = 160f

            if (categoryTotals.isNotEmpty()) {
                // 2. 🥧 PIE CHART & BREAKDOWN (Left & Right)
                paint.color = Color.BLACK
                paint.textSize = 18f
                paint.isFakeBoldText = true
                canvas.drawText("Category Breakdown", 40f, currentY, paint)

                currentY += 30f
                val pieRect = android.graphics.RectF(40f, currentY, 190f, currentY + 150f)
                var startAngle = -90f

                categoryTotals.forEach { (category, amount) ->
                    val sweepAngle = (amount / totalSpend) * 360f
                    paint.color = getNativeCategoryColor(category)
                    paint.style = Paint.Style.FILL
                    canvas.drawArc(pieRect, startAngle, sweepAngle, true, paint)
                    startAngle += sweepAngle
                }

                var legendY = currentY + 15f
                categoryTotals.take(5).forEach { (category, amount) ->
                    val percentage = (amount / totalSpend) * 100
                    paint.color = getNativeCategoryColor(category)
                    canvas.drawCircle(220f, legendY - 5f, 8f, paint)

                    paint.color = Color.DKGRAY
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText(category, 240f, legendY, paint)

                    paint.isFakeBoldText = false
                    canvas.drawText(String.format("RM %.2f (%.1f%%)", amount, percentage), 340f, legendY, paint)
                    legendY += 25f
                }

                currentY += 190f

                // 3. 📊 SLEEK BAR GRAPH
                paint.color = Color.BLACK
                paint.textSize = 18f
                paint.isFakeBoldText = true
                canvas.drawText("Top Categories (Bar Graph)", 40f, currentY, paint)

                currentY += 30f
                val maxAmount = categoryTotals.maxOf { it.second }
                val maxBarWidth = 350f

                categoryTotals.take(4).forEach { (category, amount) ->
                    paint.color = Color.DKGRAY
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText(category, 40f, currentY + 12f, paint)

                    val barWidth = (amount / maxAmount) * maxBarWidth
                    paint.color = getNativeCategoryColor(category)
                    canvas.drawRect(140f, currentY, 140f + barWidth, currentY + 16f, paint)

                    paint.color = Color.BLACK
                    paint.isFakeBoldText = false
                    canvas.drawText("RM ${amount.toInt()}", 150f + barWidth, currentY + 12f, paint)

                    currentY += 28f
                }
                currentY += 30f
            }

            // 4. 📋 TRANSACTIONS TABLE (Fits top 6 beautifully on the remaining page)
            paint.color = Color.BLACK
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Recent Transactions", 40f, currentY, paint)
            currentY += 20f

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(40f, currentY, 555f, currentY + 30f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("DATE", 50f, currentY + 20f, paint)
            canvas.drawText("MERCHANT / CATEGORY", 200f, currentY + 20f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("AMOUNT", 545f, currentY + 20f, paint)
            paint.textAlign = Paint.Align.LEFT
            currentY += 40f

            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            paint.isFakeBoldText = false
            paint.textSize = 12f

            expenses.sortedByDescending { it.timestamp }.take(6).forEachIndexed { index, exp ->
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(40f, currentY - 15f, 555f, currentY + 15f, paint)
                }

                paint.color = Color.DKGRAY
                canvas.drawText(dateFormatter.format(Date(exp.timestamp)), 50f, currentY, paint)

                val title = exp.merchantName.ifEmpty { exp.category }
                val displayTitle = if (title.length > 30) title.substring(0, 27) + "..." else title
                canvas.drawText(displayTitle, 200f, currentY, paint)

                paint.color = getNativeCategoryColor(exp.category)
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("RM ${String.format("%.2f", exp.amount)}", 545f, currentY, paint)
                paint.textAlign = Paint.Align.LEFT
                paint.isFakeBoldText = false

                currentY += 30f
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
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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