package com.finadapt.adaptivefinance.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finadapt.adaptivefinance.feature.expense.ReceiptItem

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Float,
    val category: String, //i.e food, transport
    val timestamp: Long = System.currentTimeMillis(), //saves exact the millisecond they spent it
    // 🤖 AI ENRICHED DATA (Provided ONLY by the Camera Scanner)
    // By setting these to `= ""`, Manual and Voice inputs will just ignore them safely!
    val merchantName: String = "",
    val date: String = "",
    val paymentMethod: String = "",
    val receiptImagePath: String = "",

    // 📦 The 6 scanned items (Requires the Gson TypeConverter we built!)
    val items: List<ReceiptItem> = emptyList()
)