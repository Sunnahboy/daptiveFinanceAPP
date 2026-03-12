package com.finadapt.adaptivefinance.feature.expense.history

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

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

@SuppressLint("DefaultLocale")
@Composable
fun HistoryScreen(
    allExpenses: List<ExpenseEntity>,
    isDarkMode: Boolean = false // 🟢 NEW: Dark mode state passed in
) {
    // 🟢 DYNAMIC THEME COLORS
    val bgColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val tabTrackColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    val tabActiveColor = if (isDarkMode) Color(0xFF334155) else Color.White

    var selectedFilter by remember { mutableStateOf(TimeFilter.DAILY) }
    var searchQuery by remember { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    // Chart Engine & Time Limits
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
                        String.format("%.2f", expense.amount).contains(searchQuery)
            }
            val matchesTime = expense.timestamp >= cutoffTimestamp
            matchesSearch && matchesTime
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
    ) {
        item {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.height(24.dp))

            // TIME SELECTOR TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tabTrackColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(TimeFilter.DAILY, TimeFilter.WEEKLY, TimeFilter.MONTHLY).forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val boxBgColor = if (isSelected) tabActiveColor else Color.Transparent
                    val tabTextColor = if (isSelected) textColor else subTextColor

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(boxBgColor)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTextColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // THE BAR CHART (Volume over Time)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                lineComponent(
                                    color = Color(0xFF0284C7),
                                    thickness = 16.dp,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                            )
                        ),
                        model = entryModelOf(*safeChartData.toTypedArray()),
                        startAxis = rememberStartAxis(
                            // 🟢 FIX: Axis Labels use dynamic subTextColor
                            label = textComponent(
                                color = subTextColor,
                                textSize = 10.sp
                            ),
                            valueFormatter = AxisValueFormatter { value, _ -> "RM ${value.toInt()}" }
                        ),
                        bottomAxis = rememberBottomAxis(
                            // 🟢 FIX: Axis Labels use dynamic subTextColor
                            label = textComponent(
                                color = subTextColor,
                                textSize = 10.sp
                            ),
                            valueFormatter = AxisValueFormatter { value, _ -> chartLabels.getOrNull(value.toInt()) ?: "" },
                            labelRotationDegrees = if (selectedFilter == TimeFilter.DAILY) -45f else 0f,
                            guideline = null
                        ),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // THE DONUT CHART (Categorical Breakdown)
            if (filteredExpenses.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Category Breakdown", fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        CategoryDonutChart(expenses = filteredExpenses, textColor = textColor)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // SEARCH BAR
            Text("Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by category or amount...", color = subTextColor) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- LIST SECTION ---
        if (filteredExpenses.isEmpty()) {
            item {
                Text(
                    if (searchQuery.isBlank()) "No expenses logged in this timeframe." else "No results found.",
                    color = subTextColor,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(filteredExpenses.reversed()) { expense ->
                val dateStr = dateFormatter.format(Date(expense.timestamp))
                val categoryColor = getCategoryColor(expense.category)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(12.dp).background(categoryColor, shape = RoundedCornerShape(6.dp)))
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(expense.category, fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp)
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = subTextColor, fontSize = 12.sp)
                            }
                        }

                        Text(
                            "- RM ${String.format(Locale.US, "%.2f", expense.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 🟢 Custom Canvas Donut Chart Component
@Composable
fun CategoryDonutChart(expenses: List<ExpenseEntity>, textColor: Color) {
    // 1. Group, sum, and sort expenses
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
        .toList()
        .sortedByDescending { it.second }

    val totalSpend = categoryTotals.sumOf { it.second.toDouble() }.toFloat()

    // 2. Animation State
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = expenses) { animationPlayed = true }

    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "donutAnimation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 3. THE DONUT RING ---
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentStartAngle = -90f
                val strokeWidth = 40f

                categoryTotals.forEach { (category, amount) ->
                    val sweepAngle = (amount / totalSpend) * 360f * animateSweep
                    val color = getCategoryColor(category)
                    val gap = if (categoryTotals.size > 1) 2f else 0f

                    drawArc(
                        color = color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle - gap,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentStartAngle += sweepAngle
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "RM ${String.format(Locale.US, "%.0f", totalSpend)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. THE LEGEND ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categoryTotals.forEach { (category, amount) ->
                val percentage = if (totalSpend > 0) (amount / totalSpend) * 100 else 0f
                val color = getCategoryColor(category)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${percentage.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(RM ${String.format(Locale.US, "%.0f", amount)})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}