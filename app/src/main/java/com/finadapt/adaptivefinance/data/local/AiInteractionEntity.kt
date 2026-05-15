package com.finadapt.adaptivefinance.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_interactions")
data class AiInteractionEntity(
    @PrimaryKey
    val predictionId: String,
    val strategy: String,
    val action: String,
    val notification: String,
    val visualTheme: String,
    val isFeedbackSent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)