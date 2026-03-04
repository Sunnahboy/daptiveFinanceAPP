package com.finadapt.adaptivefinance.feature.expense.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finadapt.adaptivefinance.data.local.ExpenseEntity
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    weeklyChartData: List<Float>,
    allExpenses: List<ExpenseEntity> // Pass in the real list of expenses
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(top = 48.dp, start = 20.dp, end = 20.dp)
    ) {
        Text("Analytics & History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(24.dp))

        // 🟢 THE GRAPH MOVES HERE!
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (weeklyChartData.sum() == 0f) {
                    Text("No data for the last 7 days.", color = Color.Gray, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                } else {
                    val chartEntryModel = entryModelOf(*weeklyChartData.toTypedArray())
                    Chart(
                        chart = columnChart(),
                        model = chartEntryModel,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("All Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(16.dp))

        // 🟢 THE FULL SCROLLING LEDGER
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp) // Keep it above the bottom menu!
        ) {
            if (allExpenses.isEmpty()) {
                item { Text("No expenses logged yet.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
            } else {
                items(allExpenses) { expense ->
                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(expense.timestamp))

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
                            Column {
                                Text(expense.category, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text("- RM ${expense.amount}", fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}