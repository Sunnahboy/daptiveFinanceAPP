package com.finadapt.adaptivefinance.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_interactions")
data class AiInteractionEntity(
    @PrimaryKey
    val predictionId: String,          //the UUID from AWS (Primary Key!)
    val strategy: String,
    val action: String,
    val notification: String,
    val visualTheme: String,
    val isFeedbackSent: Boolean = false, // Defaults to false until they click "Got it!"
    val timestamp: Long = System.currentTimeMillis()
)