package com.quizgame.data.model

import com.google.gson.annotations.SerializedName

data class Room(
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("playerId") val playerId: String
)

data class JoinRoomResponse(
    @SerializedName("playerId") val playerId: String,
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("players") val players: List<Player>
)

data class Player(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String,
    @SerializedName("score") val score: Int = 0,
    @SerializedName("is_connected") val isConnected: Int = 1
)

data class Question(
    @SerializedName("id") val id: Int,
    @SerializedName("category") val category: String = "",
    @SerializedName("text") val text: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("correctIndex") val correctIndex: Int
)

data class QuestionsResponse(
    @SerializedName("questions") val questions: List<Question>
)

data class CreateRoomRequest(
    @SerializedName("hostName") val hostName: String
)

data class JoinRoomRequest(
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("username") val username: String
)

data class RankingItem(
    val username: String,
    val score: Int,
    val rank: Int
)

data class ErrorResponse(
    val error: String
)
