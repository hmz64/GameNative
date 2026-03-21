package com.quizgame.ui.waiting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quizgame.data.model.Player
import com.quizgame.data.network.SocketManager
import com.quizgame.databinding.ActivityWaitingRoomBinding
import com.quizgame.ui.game.GameActivity
import org.json.JSONObject

class WaitingRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitingRoomBinding
    private lateinit var adapter: PlayerAdapter
    private val gson = Gson()

    private var roomCode = ""
    private var playerId = ""
    private var username = ""
    private var isHost = false

    companion object {
        private const val TAG = "WaitingRoom"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        playerId = intent.getStringExtra("PLAYER_ID") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""
        isHost = intent.getBooleanExtra("IS_HOST", false)

        setupUI()
        connectSocket()
    }

    private fun setupUI() {
        binding.tvRoomCode.text = roomCode
        binding.btnStartGame.visibility = if (isHost) View.VISIBLE else View.GONE
        binding.tvHostLabel.text = if (isHost) "You are the host" else "Waiting for host to start..."

        adapter = PlayerAdapter()
        binding.rvPlayers.layoutManager = LinearLayoutManager(this)
        binding.rvPlayers.adapter = adapter

        binding.btnStartGame.setOnClickListener {
            SocketManager.startGame(roomCode, playerId)
            binding.btnStartGame.isEnabled = false
        }
    }

    private fun connectSocket() {
        SocketManager.connect()
        val socket = SocketManager.getSocket()

        socket.on("connect") {
            Log.d(TAG, "Socket connected")
            SocketManager.joinRoom(roomCode, playerId, username)
        }

        socket.on("room_updated") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val playersJson = data.getJSONArray("players").toString()
                val type = object : TypeToken<List<Player>>() {}.type
                val players: List<Player> = gson.fromJson(playersJson, type)

                runOnUiThread {
                    adapter.updatePlayers(players)
                    binding.tvPlayerCount.text = "${players.size} player(s) in room"
                }
            }
        }

        socket.on("game_started") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val totalQuestions = data.optInt("totalQuestions", 10)

                runOnUiThread {
                    val intent = Intent(this, GameActivity::class.java).apply {
                        putExtra("ROOM_CODE", roomCode)
                        putExtra("PLAYER_ID", playerId)
                        putExtra("USERNAME", username)
                        putExtra("TOTAL_QUESTIONS", totalQuestions)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        socket.on("error") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = data.optString("message", "Unknown error")
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // If already connected, join room directly
        if (socket.connected()) {
            SocketManager.joinRoom(roomCode, playerId, username)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val socket = SocketManager.getSocket()
        socket.off("room_updated")
        socket.off("game_started")
        socket.off("error")
    }
}
