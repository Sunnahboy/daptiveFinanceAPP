package com.finadapt.adaptivefinance.data.remote
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

//1. The Data we send to the aws server instance
data class ContextRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("test_group")
    val testGroup: String = "adaptive", // 🟢 NEW: Tells the DB they are in the AI group

    @SerializedName("amount")
    val amount: Float,                  // 🟢 NEW: The raw RM amount

    @SerializedName("category")
    val category: String,

    @SerializedName("features")
    val features: Map<String, Float>,

)

//2. The data we get back from the AI
data class AiResponse(
    @SerializedName("prediction_id")
    val predictionId: String? = null,  // <--- This was missing!

    @SerializedName("strategy")
    val recommendedStrategy: String? = null,

    @SerializedName("notification")
    val gamificationMessage: String? = null,

    @SerializedName("action")
    val action: String? = null,

    @SerializedName("visual_theme")
    val visualTheme: String? = null
)
//3 The Feedback Payload for Phase 3
data class FeedbackRequest(
    @SerializedName("prediction_id")
    val predictionId: String,
    @SerializedName("reward")
    val reward: Float
)

// LeaderBoard data models
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

data class LeaderboardEntry(
    @SerializedName("anonymous_name")
    val anonymousName: String,
    @SerializedName("xp")
    val xp: Int,
    @SerializedName("tier")
    val tier: String
)

data class LeaderboardTopResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: List<LeaderboardEntry>
)



//4. The exact FastAPI Endpoint
interface FastApiInterface{
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
    suspend fun syncLeaderboardXp(@Body request: LeaderboardUpdateRequest): Response<Unit>
    //Fetch the Top 50 Users
    @GET("gamification/v1/leaderboard/top")
    suspend fun getLeaderboardTop(): Response<LeaderboardTopResponse>
}