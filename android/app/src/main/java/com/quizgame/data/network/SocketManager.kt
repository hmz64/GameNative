package com.quizgame.data.network

import android.util.Log
import com.quizgame.utils.NetworkConfig
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private const val TAG = "SocketManager"
    private var socket: Socket? = null

    fun getSocket(): Socket {
        if (socket == null || !socket!!.connected()) {
            try {
                val options = IO.Options().apply {
                    forceNew = false
                    reconnection = true
                    reconnectionAttempts = 10
                    reconnectionDelay = 1000
                    timeout = 10000
                }
                socket = IO.socket(NetworkConfig.BASE_URL, options)
                Log.d(TAG, "Socket created: ${NetworkConfig.BASE_URL}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating socket: ${e.message}")
                throw e
            }
        }
        return socket!!
    }

    fun connect() {
        val s = getSocket()
        if (!s.connected()) {
            s.connect()
            Log.d(TAG, "Socket connecting...")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        Log.d(TAG, "Socket disconnected and cleared")
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun joinRoom(roomCode: String, playerId: String, username: String) {
        val data = JSONObject().apply {
            put("roomCode", roomCode)
            put("playerId", playerId)
            put("username", username)
        }
        getSocket().emit("join_room", data)
        Log.d(TAG, "Emitting join_room: $data")
    }

    fun startGame(roomCode: String, playerId: String) {
        val data = JSONObject().apply {
            put("roomCode", roomCode)
            put("playerId", playerId)
        }
        getSocket().emit("start_game", data)
        Log.d(TAG, "Emitting start_game: $data")
    }

    fun submitAnswer(roomCode: String, playerId: String, questionIndex: Int, answerIndex: Int, timeLeft: Int) {
        val data = JSONObject().apply {
            put("roomCode", roomCode)
            put("playerId", playerId)
            put("questionIndex", questionIndex)
            put("answerIndex", answerIndex)
            put("timeLeft", timeLeft)
        }
        getSocket().emit("submit_answer", data)
        Log.d(TAG, "Emitting submit_answer: $data")
    }
}
