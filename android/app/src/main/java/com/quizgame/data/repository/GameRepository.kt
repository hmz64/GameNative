package com.quizgame.data.repository

import com.quizgame.data.model.*
import com.quizgame.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository {

    private val api = RetrofitClient.apiService

    suspend fun createRoom(hostName: String): Result<Room> = withContext(Dispatchers.IO) {
        try {
            val response = api.createRoom(CreateRoomRequest(hostName))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomCode: String, username: String): Result<JoinRoomResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.joinRoom(JoinRoomRequest(roomCode, username))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to join room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestions(): Result<List<Question>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getQuestions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.questions)
            } else {
                Result.failure(Exception("Failed to fetch questions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
