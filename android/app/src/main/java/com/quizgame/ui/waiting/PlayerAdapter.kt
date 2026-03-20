package com.quizgame.ui.waiting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.quizgame.R
import com.quizgame.data.model.Player

class PlayerAdapter : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    private val players = mutableListOf<Player>()

    fun updatePlayers(newPlayers: List<Player>) {
        players.clear()
        players.addAll(newPlayers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlayerName: TextView = itemView.findViewById(R.id.tvPlayerName)
        private val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(player: Player) {
            tvPlayerName.text = player.username
            ivStatus.setImageResource(
                if (player.isConnected == 1) R.drawable.ic_connected else R.drawable.ic_disconnected
            )
        }
    }
}
