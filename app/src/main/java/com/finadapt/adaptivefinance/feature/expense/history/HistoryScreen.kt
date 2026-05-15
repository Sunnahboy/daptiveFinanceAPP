
package com.finadapt.adaptivefinance.feature.expense.history

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food", "dining", "groceries" -> Color(0xFFEF4444)
        "transport", "gas", "parking" -> Color(0xFF3B82F6)
        "entertainment", "movies", "games" -> Color(0xFF8B5CF6)
        "shopping", "clothes", "accessories" -> Color(0xFFF59E0B)
        "utilities", "bills", "rent" -> Color(0xFF10B981)
        "health", "medical", "fitness" -> Color(0xFFF472B6)
        else -> Color(0xFF06B6D4)
    }
}

enum class TimeFilter { DAILY, WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun HistoryScreen(
    allExpenses: List<ExpenseEntity>,
    isDarkMode: Boolean = false,
    //Hooks to tell the ViewModel to Edit or Delete!
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
) {
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val tabTrackColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val tabActiveColor = if (isDarkMode) Color(0xFF334155) else Color.White

    var selectedFilter by remember { mutableStateOf(TimeFilter.DAILY) }
    var searchQuery by remember { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    var selectedExpenseForDetails by remember { mutableStateOf<ExpenseEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    //State for the Edit Dialog
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }


    val (safeChartData, chartLabels, cutoffTimestamp) = remember(allExpenses, selectedFilter) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayMidnight = cal.timeInMillis
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        when (selectedFilter) {
            TimeFilter.DAILY -> {
                val totals = FloatArray(7)
                val labels = mutableListOf<String>()
                val format = SimpleDateFormat("EEE", Locale.getDefault())
                val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis

                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    labels.add(format.format(c.time))
                }
                allExpenses.forEach { exp ->
                    val daysAgo = ((todayMidnight - exp.timestamp) / (1000 * 60 * 60 * 24)).toInt()
                    val idx = if (exp.timestamp >= todayMidnight) 6 else 6 - (daysAgo + 1)
                    if (idx in 0..6) totals[idx] += exp.amount
                }
                if (totals.sum() == 0f) totals[0] = 0.01f
                Triple(totals.toList(), labels, cutoff)
            }
            TimeFilter.WEEKLY -> {
                val totals = FloatArray(4)
                val labels = listOf("3W Ago", "2W Ago", "Last Wk", "This Wk")
                val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -27) }.timeInMillis

                allExpenses.forEach { exp ->
                    val daysAgo = ((todayMidnight - exp.timestamp) / (1000 * 60 * 60 * 24)).toInt()
                    val weeksAgo = if (exp.timestamp >= todayMidnight) 0 else (daysAgo + 1) / 7
                    val idx = 3 - weeksAgo
                    if (idx in 0..3) totals[idx] += exp.amount
                }
                if (totals.sum() == 0f) totals[0] = 0.01f
                Triple(totals.toList(), labels, cutoff)
            }
            TimeFilter.MONTHLY -> {
                val totals = FloatArray(6)
                val labels = mutableListOf<String>()
                val format = SimpleDateFormat("MMM", Locale.getDefault())
                val cutoff = Calendar.getInstance().apply { add(Calendar.MONTH, -5); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

                for (i in 5 downTo 0) {
                    val c = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                    labels.add(format.format(c.time))
                }
                allExpenses.forEach { exp ->
                    val c = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                    val expMonth = c.get(Calendar.MONTH)
                    val expYear = c.get(Calendar.YEAR)
                    val monthDiff = (currentYear - expYear) * 12 + (currentMonth - expMonth)
                    val idx = 5 - monthDiff
                    if (idx in 0..5) totals[idx] += exp.amount
                }
                if (totals.sum() == 0f) totals[0] = 0.01f
                Triple(totals.toList(), labels, cutoff)
            }
        }
    }

    val filteredExpenses = remember(searchQuery, allExpenses, selectedFilter) {
        allExpenses.filter { expense ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                expense.category.lowercase().contains(searchQuery.lowercase()) ||
                        String.format("%.2f", expense.amount).contains(searchQuery) ||
                        expense.merchantName.lowercase().contains(searchQuery.lowercase())
            }
            val matchesTime = expense.timestamp >= cutoffTimestamp
            matchesSearch && matchesTime
        }
    }

    Scaffold(
        containerColor = bgColor,
    ) { innerPadding ->
        //Wrap the entire screen content in a Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
            ) {

                item {
                    Text("Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth().background(tabTrackColor, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(TimeFilter.DAILY, TimeFilter.WEEKLY, TimeFilter.MONTHLY).forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val boxBgColor = if (isSelected) tabActiveColor else Color.Transparent
                            val tabTextColor = if (isSelected) textColor else subTextColor

                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(boxBgColor).clickable { selectedFilter = filter }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(text = filter.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = tabTextColor, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp), shape = RoundedCornerShape(16.dp)) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Chart(chart = columnChart(columns = listOf(lineComponent(color = Color(0xFF0284C7), thickness = 16.dp, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))), model = entryModelOf(*safeChartData.toTypedArray()), startAxis = rememberStartAxis(label = textComponent(color = subTextColor, textSize = 10.sp), valueFormatter = AxisValueFormatter { value, _ -> "RM ${value.toInt()}" }), bottomAxis = rememberBottomAxis(label = textComponent(color = subTextColor, textSize = 10.sp), valueFormatter = AxisValueFormatter { value, _ -> chartLabels.getOrNull(value.toInt()) ?: "" }, labelRotationDegrees = if (selectedFilter == TimeFilter.DAILY) -45f else 0f, guideline = null), modifier = Modifier.fillMaxWidth().height(180.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredExpenses.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Category Breakdown", fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                CategoryDonutChart(expenses = filteredExpenses, textColor = textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search by category or amount...", color = subTextColor) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = TextFieldDefaults.colors(focusedContainerColor = cardBg, unfocusedContainerColor = cardBg, focusedTextColor = textColor, unfocusedTextColor = textColor, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- LIST SECTION ---
                if (filteredExpenses.isEmpty()) {
                    item { Text(if (searchQuery.isBlank()) "No expenses logged in this timeframe." else "No results found.", color = subTextColor, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(filteredExpenses.reversed()) { expense ->
                        val dateStr = dateFormatter.format(Date(expense.timestamp))
                        val categoryColor = getCategoryColor(expense.category)
                        val hasReceipt = expense.receiptImagePath.isNotEmpty() || expense.items.isNotEmpty()

                        var showDropdown by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = hasReceipt) { selectedExpenseForDetails = expense },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(12.dp).background(categoryColor, shape = RoundedCornerShape(6.dp)))
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = expense.merchantName.ifEmpty { expense.category },
                                            fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp, maxLines = 1
                                        )
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor, fontSize = 12.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasReceipt) {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Has Receipt", tint = subTextColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text("- RM ${String.format(Locale.US, "%.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = categoryColor, fontSize = 14.sp)

                                    // The Three-Dots Menu Button
                                    Box {
                                        IconButton(onClick = { showDropdown = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = subTextColor)
                                        }

                                        DropdownMenu(
                                            expanded = showDropdown,
                                            onDismissRequest = { showDropdown = false },
                                            modifier = Modifier.background(cardBg)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit", color = textColor) },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = textColor) },
                                                onClick = {
                                                    showDropdown = false
                                                    expenseToEdit = expense
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = Color.Red) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) },
                                                onClick = {
                                                    showDropdown = false
                                                    onDeleteExpense(expense)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }


        // --- THE EDIT DIALOG ---
        if (expenseToEdit != null) {
            var editAmount by remember { mutableStateOf(expenseToEdit!!.amount.toString()) }
            var editCategory by remember { mutableStateOf(expenseToEdit!!.category) }

            AlertDialog(
                onDismissRequest = { expenseToEdit = null },
                containerColor = cardBg,
                title = { Text("Edit Expense", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = { Text("Amount (RM)", color = subTextColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Category", color = subTextColor) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newAmount = editAmount.toFloatOrNull()
                            if (newAmount != null && editCategory.isNotBlank()) {
                                // Create an updated copy of the entity
                                val updatedExpense = expenseToEdit!!.copy(
                                    amount = newAmount,
                                    category = editCategory
                                )
                                onEditExpense(updatedExpense) // Tells ViewModel to update
                                expenseToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expenseToEdit = null }) {
                        Text("Cancel", color = subTextColor)
                    }
                }
            )
        }

        //Keep the digital receipt Bottom Sheet perfectly identical
        if (selectedExpenseForDetails != null) {
            val expense = selectedExpenseForDetails!!
            ModalBottomSheet(onDismissRequest = { selectedExpenseForDetails = null }, sheetState = sheetState, containerColor = cardBg) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.merchantName.ifEmpty { expense.category }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                            Text(expense.date.ifEmpty { dateFormatter.format(Date(expense.timestamp)) }, color = subTextColor, fontSize = 14.sp)
                        }
                        if (expense.receiptImagePath.isNotEmpty()) {
                            var showFullImage by remember { mutableStateOf(false) }
                            AsyncImage(model = File(expense.receiptImagePath), contentDescription = "Receipt Thumbnail", contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).clickable { showFullImage = true })
                            if (showFullImage) {
                                Dialog(onDismissRequest = { showFullImage = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
                                        AsyncImage(model = File(expense.receiptImagePath), contentDescription = "Full Receipt", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        IconButton(onClick = { showFullImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = subTextColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (expense.items.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(expense.items) { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1)
                                        Text(item.category, fontSize = 12.sp, color = getCategoryColor(item.category))
                                    }
                                    Text("RM ${String.format(Locale.US, "%.2f", item.amount)}", fontWeight = FontWeight.Bold, color = textColor)
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = subTextColor.copy(alpha = 0.1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("This expense was logged manually without itemized details.", color = subTextColor, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("RM ${String.format(Locale.US, "%.2f", expense.amount)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = getCategoryColor(expense.category))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }


// CategoryDonutChart
@Composable
fun CategoryDonutChart(expenses: List<ExpenseEntity>, textColor: Color) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }.toList().sortedByDescending { it.second }
    val totalSpend = categoryTotals.sumOf { it.second.toDouble() }.toFloat()
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = expenses) { animationPlayed = true }
    val animateSweep by animateFloatAsState(targetValue = if (animationPlayed) 1f else 0f, animationSpec = tween(durationMillis = 1000), label = "donutAnimation")
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentStartAngle = -90f
                val strokeWidth = 40f
                categoryTotals.forEach { (category, amount) ->
                    val sweepAngle = (amount / totalSpend) * 360f * animateSweep
                    val color = getCategoryColor(category)
                    val gap = if (categoryTotals.size > 1) 2f else 0f
                    drawArc(color = color, startAngle = currentStartAngle, sweepAngle = sweepAngle - gap, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
                    currentStartAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = Color.Gray, fontSize = 12.sp)
                Text(text = "RM ${String.format(Locale.US, "%.0f", totalSpend)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = textColor)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categoryTotals.forEach { (category, amount) ->
                val percentage = if (totalSpend > 0) (amount / totalSpend) * 100 else 0f
                val color = getCategoryColor(category)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${percentage.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "(RM ${String.format(Locale.US, "%.0f", amount)})", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}