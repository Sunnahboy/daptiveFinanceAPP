package com.finadapt.adaptivefinance.data.local

import androidx.room.TypeConverter
import com.finadapt.adaptivefinance.feature.expense.ReceiptItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    //When saving TO the database (List -> String)
    @TypeConverter
    fun fromReceiptItemList(items: List<ReceiptItem>): String {
        return gson.toJson(items)
    }

    //When reading FROM the database (String -> List)
    @TypeConverter
    fun toReceiptItemList(itemsString: String): List<ReceiptItem> {
        if (itemsString.isEmpty()) return emptyList()
        val listType = object : TypeToken<List<ReceiptItem>>() {}.type
        return gson.fromJson(itemsString, listType)
    }
}