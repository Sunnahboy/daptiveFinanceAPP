package com.finadapt.adaptivefinance.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finadapt.adaptivefinance.feature.expense.ReceiptItem

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Float,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val merchantName: String = "",
    val date: String = "",
    val paymentMethod: String = "",
    val receiptImagePath: String = "",

    //scanned items uses Gson TypeConverter
    val items: List<ReceiptItem> = emptyList()
)