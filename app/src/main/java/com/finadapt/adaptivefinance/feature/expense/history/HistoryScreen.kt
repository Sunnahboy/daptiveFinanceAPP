package com.finadapt.adaptivefinance.feature.expense.history

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

// 🟢 Category Colors for Visual Distinction
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food", "dining", "groceries" -> Color(0xFFEF4444) // Red
        "transport", "gas", "parking" -> Color(0xFF3B82F6) // Blue
        "entertainment", "movies", "games" -> Color(0xFF8B5CF6) // Purple
        "shopping", "clothes", "accessories" -> Color(0xFFF59E0B) // Amber
        "utilities", "bills", "rent" -> Color(0xFF10B981) // Green
        "health", "medical", "fitness" -> Color(0xFFF472B6) // Pink
        else -> Color(0xFF06B6D4) // Cyan (default)
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun HistoryScreen(
    weeklyChartData: List<Float>, // Kept for ViewModel compatibility
    allExpenses: List<ExpenseEntity>
) {
    // 🟢 Dynamic day labels
    val dayLabels = remember {
        val labels = mutableListOf<String>()
        val format = SimpleDateFormat("EEE", Locale.getDefault())
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            labels.add(if (i == 0) "Today" else format.format(cal.time))
        }
        labels
    }

    // 🟢 Strict Midnight Math (Fixes the timezone clumping bug!)
    val safeChartData = remember(allExpenses) {
        val dailyTotals = FloatArray(7)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayMidnight = cal.timeInMillis

        allExpenses.forEach { expense ->
            val diff = todayMidnight - expense.timestamp
            val daysAgo = (diff / (1000 * 60 * 60 * 24)).toInt()

            // Forces anything before midnight into the correct past day
            val index = if (expense.timestamp >= todayMidnight) 6 else 6 - (daysAgo + 1)

            if (index in 0..6) {
                dailyTotals[index] += expense.amount
            }
        }

        if (dailyTotals.sum() == 0f) dailyTotals[0] = 0.01f
        dailyTotals.toList()
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredExpenses = remember(searchQuery, allExpenses) {
        if (searchQuery.isBlank()) {
            allExpenses
        } else {
            allExpenses.filter { expense ->
                expense.category.lowercase().contains(searchQuery.lowercase()) ||
                        String.format("%.2f", expense.amount).contains(searchQuery)
            }
        }
    }

    // 🟢 ENTIRE SCREEN IS NOW SCROLLABLE
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
    ) {

        // --- HEADER SECTION (Inside the scroll list!) ---
        item {
            Text(
                "Analytics & History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 🟢 CHART CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Chart(
                        chart = columnChart(),
                        model = entryModelOf(*safeChartData.toTypedArray()),
                        startAxis = rememberStartAxis(
                            valueFormatter = AxisValueFormatter { value, _ -> "RM ${value.toInt()}" }
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = AxisValueFormatter { value, _ -> dayLabels.getOrNull(value.toInt()) ?: "" },
                            labelRotationDegrees = -45f,
                            guideline = null
                        ),
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🟢 SEARCH BAR
            Text(
                "All Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by category or amount...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, // Looks cleaner without the underline
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
                    if (searchQuery.isBlank()) "No expenses logged yet." else "No results found.",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(filteredExpenses.reversed()) { expense ->
                val dateStr = dateFormatter.format(Date(expense.timestamp))
                val categoryColor = getCategoryColor(expense.category)

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // 🟢 Your beautiful Color Dot!
                            Box(modifier = Modifier.size(12.dp).background(categoryColor, shape = RoundedCornerShape(6.dp)))
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(expense.category, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), fontSize = 14.sp)
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        Text(
                            "- RM ${String.format("%.2f", expense.amount)}",
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