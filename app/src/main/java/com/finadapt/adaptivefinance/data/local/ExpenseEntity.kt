package com.finadapt.adaptivefinance.data.local


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Float,
    val category: String, //i.e food, transport
    val timestamp: Long = System.currentTimeMillis() //saves exact the millisecond they spent it

)