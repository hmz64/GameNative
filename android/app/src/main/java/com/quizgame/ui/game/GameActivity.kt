package com.quizgame.ui.game

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quizgame.R
import com.quizgame.data.model.RankingItem
import com.quizgame.data.network.SocketManager
import com.quizgame.databinding.ActivityGameBinding
import com.quizgame.ui.result.ResultActivity
import com.quizgame.utils.GameConstants
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val gson = Gson()

    private var roomCode = ""
    private var playerId = ""
    private var username = ""
    private var totalQuestions = 10
    private var currentQuestionIndex = 0
    private var selectedAnswerIndex = -1
    private var timeLeft = 0
    private var countDownTimer: CountDownTimer? = null
    private lateinit var answerButtons: List<Button>

    companion object {
        private const val TAG = "GameActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomCode = intent.getStringExtra("ROOM_CODE") ?: ""
        playerId = intent.getStringExtra("PLAYER_ID") ?: ""
        username = intent.getStringExtra("USERNAME") ?: ""
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 10)

        answerButtons = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)

        setupSocketListeners()
        setupAnswerButtons()
    }

    private fun setupAnswerButtons() {
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (selectedAnswerIndex == -1) {
                    selectedAnswerIndex = index
                    countDownTimer?.cancel()
                    disableAllButtons()
                    button.setBackgroundColor(Color.parseColor("#FFA726")) // Orange for selected
                    SocketManager.submitAnswer(roomCode, playerId, currentQuestionIndex, index, timeLeft)
                }
            }
        }
    }

    private fun setupSocketListeners() {
        val socket = SocketManager.getSocket()

        socket.on("new_question") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val index = data.getInt("index")
                val text = data.getString("text")
                val optionsJson = data.getJSONArray("options")
                val timeLimit = data.optInt("timeLimit", GameConstants.TIME_LIMIT)

                val options = mutableListOf<String>()
                for (i in 0 until optionsJson.length()) {
                    options.add(optionsJson.getString(i))
                }

                runOnUiThread {
                    showQuestion(index, text, options, timeLimit)
                }
            }
        }

        socket.on("answer_result") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val correctIndex = data.getInt("correctIndex")
                val scoresJson = data.optJSONObject("scores")

                runOnUiThread {
                    showAnswerResult(correctIndex, scoresJson)
                }
            }
        }

        socket.on("leaderboard") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val rankingsJson = data.getJSONArray("rankings").toString()
                val type = object : TypeToken<List<RankingItem>>() {}.type
                val rankings: List<RankingItem> = gson.fromJson(rankingsJson, type)

                runOnUiThread {
                    showLeaderboardOverlay(rankings)
                }
            }
        }

        socket.on("game_over") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val rankingsJson = data.getJSONArray("finalRankings").toString()

                runOnUiThread {
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("RANKINGS_JSON", rankingsJson)
                        putExtra("USERNAME", username)
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
                    if (message.contains("Host disconnected")) {
                        AlertDialog.Builder(this)
                            .setTitle("Game Ended")
                            .setMessage(message)
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showQuestion(index: Int, text: String, options: List<String>, timeLimit: Int) {
        currentQuestionIndex = index
        selectedAnswerIndex = -1

        binding.tvQuestionNumber.text = "Question ${index + 1}/$totalQuestions"
        binding.tvQuestion.text = text

        answerButtons.forEachIndexed { i, button ->
            button.isEnabled = true
            button.text = "${('A' + i)}. ${options.getOrElse(i) { "" }}"
            button.setBackgroundColor(Color.parseColor("#6200EE"))
        }

        binding.tvTimer.visibility = View.VISIBLE
        startTimer(timeLimit)
    }

    private fun startTimer(timeLimit: Int) {
        countDownTimer?.cancel()
        timeLeft = timeLimit

        binding.progressTimer.max = timeLimit
        binding.progressTimer.progress = timeLimit

        countDownTimer = object : CountDownTimer((timeLimit * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000).toInt()
                binding.tvTimer.text = "$timeLeft"
                binding.progressTimer.progress = timeLeft

                if (timeLeft <= 5) {
                    binding.tvTimer.setTextColor(Color.RED)
                } else {
                    binding.tvTimer.setTextColor(Color.WHITE)
                }
            }

            override fun onFinish() {
                timeLeft = 0
                binding.tvTimer.text = "0"
                binding.progressTimer.progress = 0
                if (selectedAnswerIndex == -1) {
                    disableAllButtons()
                    // Auto-submit with no answer (-1)
                    SocketManager.submitAnswer(roomCode, playerId, currentQuestionIndex, -1, 0)
                }
            }
        }.start()
    }

    private fun disableAllButtons() {
        answerButtons.forEach { it.isEnabled = false }
    }

    private fun showAnswerResult(correctIndex: Int, scores: JSONObject?) {
        // Highlight correct answer in green
        if (correctIndex in answerButtons.indices) {
            answerButtons[correctIndex].setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        // Highlight wrong selected answer in red
        if (selectedAnswerIndex != -1 && selectedAnswerIndex != correctIndex) {
            answerButtons[selectedAnswerIndex].setBackgroundColor(Color.parseColor("#F44336"))
        }

        // Show score
        val myScore = scores?.optInt(username, 0) ?: 0
        binding.tvScoreInfo.text = "Score: $myScore"
        binding.tvScoreInfo.visibility = View.VISIBLE
    }

    private fun showLeaderboardOverlay(rankings: List<RankingItem>) {
        binding.layoutLeaderboard.visibility = View.VISIBLE
        val sb = StringBuilder()
        rankings.forEach { r ->
            val marker = if (r.username == username) " ★" else ""
            sb.appendLine("${r.rank}. ${r.username}: ${r.score} pts$marker")
        }
        binding.tvLeaderboardContent.text = sb.toString()

        // Auto-dismiss after 3 seconds
        binding.layoutLeaderboard.postDelayed({
            binding.layoutLeaderboard.visibility = View.GONE
            binding.tvScoreInfo.visibility = View.GONE
        }, GameConstants.LEADERBOARD_DISPLAY_TIME)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        val socket = SocketManager.getSocket()
        socket.off("new_question")
        socket.off("answer_result")
        socket.off("leaderboard")
        socket.off("game_over")
        socket.off("error")
    }
}
