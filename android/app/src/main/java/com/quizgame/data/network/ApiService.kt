package com.quizgame.data.network

import com.quizgame.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("/api/room/create")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<Room>

    @POST("/api/room/join")
    suspend fun joinRoom(@Body request: JoinRoomRequest): Response<JoinRoomResponse>

    @GET("/api/questions")
    suspend fun getQuestions(): Response<QuestionsResponse>
}
