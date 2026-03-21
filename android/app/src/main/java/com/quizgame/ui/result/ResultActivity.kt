package com.quizgame.ui.result

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quizgame.R
import com.quizgame.data.model.RankingItem
import com.quizgame.data.network.SocketManager
import com.quizgame.databinding.ActivityResultBinding
import com.quizgame.ui.main.MainActivity

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rankingsJson = intent.getStringExtra("RANKINGS_JSON") ?: "[]"
        val username = intent.getStringExtra("USERNAME") ?: ""

        val listType = object : TypeToken<List<RankingItem>>() {}.type
        val rankings: List<RankingItem> = gson.fromJson(rankingsJson, listType)

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = ResultAdapter(rankings, username)

        if (rankings.isNotEmpty()) {
            binding.tvWinner.text = "🏆 Winner: ${rankings[0].username} with ${rankings[0].score} pts!"
        }

        binding.btnPlayAgain.setOnClickListener {
            SocketManager.disconnect()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        binding.btnShareResult.setOnClickListener {
            val sb = StringBuilder("🎮 Quiz Game Results:\n\n")
            rankings.forEach { r ->
                sb.appendLine("${r.rank}. ${r.username}: ${r.score} pts")
            }
            sb.appendLine("\nPlay with me!")

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())
            startActivity(Intent.createChooser(shareIntent, "Share Results"))
        }
    }

    class ResultAdapter(
        private val rankings: List<RankingItem>,
        private val currentUsername: String
    ) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
            return ResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.bind(rankings[position], currentUsername)
        }

        override fun getItemCount() = rankings.size

        class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
            private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
            private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

            fun bind(item: RankingItem, currentUsername: String) {
                tvRank.text = "#${item.rank}"
                tvUsername.text = item.username
                tvScore.text = "${item.score} pts"

                if (item.username == currentUsername) {
                    itemView.setBackgroundColor(Color.parseColor("#FFF9C4"))
                    tvUsername.setTextColor(Color.parseColor("#6200EE"))
                } else {
                    itemView.setBackgroundColor(Color.TRANSPARENT)
                    tvUsername.setTextColor(Color.BLACK)
                }
            }
        }
    }
}
