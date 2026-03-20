package com.quizgame.utils

object NetworkConfig {
    // For Android Emulator: use 10.0.2.2 to access host machine's localhost
    // For physical device: use your computer's local IP address (e.g., 192.168.1.x)
    var BASE_URL = "http://10.0.2.2:3000"
}

object GameConstants {
    const val TIME_LIMIT = 15 // seconds per question
    const val LEADERBOARD_DISPLAY_TIME = 3000L // ms
    const val ROOM_CODE_LENGTH = 6
}
