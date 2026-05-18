package com.finadapt.adaptivefinance.data.remote
import com.finadapt.adaptivefinance.feature.expense.ParsedReceipt
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST



/**
 * =====================================
 * REMOTE API CONTRACT (NETWORK LAYER)
 * ==========================================
 * This file acts as the bridge between the Android app and the remote cloud server (FastAPI).
 * * 1. DATA MODELS: It defines the JSON structures (Requests and Responses) so Retrofit knows
 * how to pack and unpack data when talking to the server.
 * 2. FAST API INTERFACE: It maps standard Kotlin functions to actual HTTP endpoints
 * (GET, POST) to handle AI predictions, Leaderboard syncing, and Chat features.
 */


//Data sent to the server instance
data class ContextRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("test_group")
    val testGroup: String = "adaptive",
    @SerializedName("amount")
    val amount: Float,
    @SerializedName("category")
    val category: String,
    @SerializedName("features")
    val features: Map<String, Float>,
)

//The data we get back from the bandit
data class AiResponse(
    @SerializedName("prediction_id")
    val predictionId: String? = null,
    @SerializedName("strategy")
    val recommendedStrategy: String? = null,
    @SerializedName("notification")
    val gamificationMessage: String? = null,
    @SerializedName("action")
    val action: String? = null,
    @SerializedName("visual_theme")
    val visualTheme: String? = null
)

//Feedback Payload
data class FeedbackRequest(
    @SerializedName("prediction_id")
    val predictionId: String,
    @SerializedName("reward")
    val reward: Float
)

//LeaderBoard data models
data class LeaderboardUpdateRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("anonymous_name")
    val anonymousName: String,
    @SerializedName("xp")
    val xp: Int,
    @SerializedName("tier")
    val tier: String
)

data class CheerRequest(
    @SerializedName("target_user_id")
    val targetUserId: String
)

data class HallOfFameEntry(
    @SerializedName("winner_name")
    val anonymousName: String,
    @SerializedName("xp_earned")
    val xp: Int,
    @SerializedName("tier_reached")
    val tier: String,
    @SerializedName("week_of")
    val weekOf: String
)

data class LeaderboardHistoryResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<HallOfFameEntry>
)

data class LeaderboardEntry(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("anonymous_name")
    val anonymousName: String,

    @SerializedName("xp")
    val xp: Int,

    @SerializedName("tier")
    val tier: String,

    // 🟢 ADD THIS LINE: Now Android knows how to read the cheers!
    @SerializedName("cheers")
    val cheers: Int = 0
)

data class LeaderboardTopResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<LeaderboardEntry>
)

//KTOR rate-Limiter AI Endpoints
data class ChatRequest(
    @SerializedName("prompt")
    val prompt: String
)

data class ChatResponse(
    @SerializedName("message")
    val message: String
)

data class ReceiptRequest(
    @SerializedName("raw_text")
    val rawText: String
)

//FastAPI Endpoint
interface FastApiInterface {
    @POST("predict/v1/context")
    suspend fun getAiGamification(
        @Header("X-API-Token") token: String,
        @Body request: ContextRequest
    ): AiResponse

    @POST("predict/v1/feedback")
    suspend fun sendFeedback(
        @Header("X-API-Token") token: String,
        @Body request: FeedbackRequest
    )

    @POST("gamification/v1/leaderboard/update")
    suspend fun syncLeaderboardXp(
        @Header("X-API-Token") token: String,
        @Body request: LeaderboardUpdateRequest): Response<Unit>

    @GET("gamification/v1/leaderboard/top")
    suspend fun getLeaderboardTop(
        @Header("X-API-Token") token: String
    ): Response<LeaderboardTopResponse>

    @POST("gamification/v1/leaderboard/cheer")
    suspend fun sendCheer(
        @Header("X-API-Token") token: String,
        @Body request: CheerRequest
    ): Response<Unit>

    @GET("gamification/v1/leaderboard/history")
    suspend fun getLeaderboardHistory(
        @Header("X-API-Token") token: String
    ): Response<LeaderboardHistoryResponse>

    @POST("api/chat")
    suspend fun askFinancialAi(
        @Header("X-User-Id") userId: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("api/receipt")
    suspend fun parseReceipt(
        @Header("X-User-Id") userId: String,
        @Body request: ReceiptRequest
    ): Response<ParsedReceipt>
}