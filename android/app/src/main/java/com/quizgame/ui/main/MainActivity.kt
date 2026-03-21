package com.quizgame.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.quizgame.R
import com.quizgame.databinding.ActivityMainBinding
import com.quizgame.ui.waiting.WaitingRoomActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnCreateRoom.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                binding.etUsername.error = "Please enter your name"
                return@setOnClickListener
            }
            viewModel.createRoom(username)
        }

        binding.btnJoinRoom.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                binding.etUsername.error = "Please enter your name"
                return@setOnClickListener
            }
            showJoinRoomDialog(username)
        }
    }

    private fun showJoinRoomDialog(username: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_room, null)
        val etRoomCode = dialogView.findViewById<EditText>(R.id.etRoomCode)

        AlertDialog.Builder(this)
            .setTitle("Join Room")
            .setView(dialogView)
            .setPositiveButton("Join") { _, _ ->
                val roomCode = etRoomCode.text.toString().trim().uppercase()
                if (roomCode.length == 6) {
                    viewModel.joinRoom(roomCode, username)
                } else {
                    Toast.makeText(this, "Please enter a valid 6-character room code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.createRoomResult.observe(this) { result ->
            result.onSuccess { room ->
                navigateToWaitingRoom(
                    roomCode = room.roomCode,
                    playerId = room.playerId,
                    username = binding.etUsername.text.toString().trim(),
                    isHost = true
                )
            }
            result.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.joinRoomResult.observe(this) { result ->
            result.onSuccess { response ->
                navigateToWaitingRoom(
                    roomCode = response.roomCode,
                    playerId = response.playerId,
                    username = binding.etUsername.text.toString().trim(),
                    isHost = false
                )
            }
            result.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.btnCreateRoom.isEnabled = !loading
            binding.btnJoinRoom.isEnabled = !loading
        }
    }

    private fun navigateToWaitingRoom(roomCode: String, playerId: String, username: String, isHost: Boolean) {
        val intent = Intent(this, WaitingRoomActivity::class.java).apply {
            putExtra("ROOM_CODE", roomCode)
            putExtra("PLAYER_ID", playerId)
            putExtra("USERNAME", username)
            putExtra("IS_HOST", isHost)
        }
        startActivity(intent)
    }
}
