# Multiplayer Quiz/Trivia Game

A real-time multiplayer quiz/trivia game with an Android native client and Node.js backend server.

Players join a room using a 6-character code, answer questions simultaneously with a countdown timer, and see a live leaderboard after each question.

## Project Structure

```
.
├── server/          # Node.js backend (Express + Socket.IO + SQLite)
├── android/         # Android native app (Kotlin)
└── README.md
```

---

## Server Setup

### Prerequisites
- Node.js (v18+)

### Installation & Running

```bash
cd server
npm install
node index.js
```

The server starts on **port 3000**.

- REST API: `http://localhost:3000/api/`
- Socket.IO: `ws://localhost:3000`
- Health check: `http://localhost:3000/health`

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/room/create` | Create a new room (`{ "hostName": "..." }`) |
| POST | `/api/room/join` | Join a room (`{ "roomCode": "...", "username": "..." }`) |
| GET | `/api/questions` | Get all trivia questions |
| GET | `/api/room/:code` | Get room info (debug) |
| GET | `/health` | Server health check |

### Socket.IO Events

**Client to Server:**
- `join_room` — Join a room socket channel
- `start_game` — Host starts the game
- `submit_answer` — Player submits an answer

**Server to Client:**
- `room_updated` — Player list changed
- `game_started` — Game has begun
- `new_question` — New question sent
- `answer_result` — Answer results after timer/all answered
- `leaderboard` — Rankings after each question
- `game_over` — Final rankings

### Game Rules
- 10 random questions per game (from pool of 20)
- 15 seconds per question
- Scoring: 100 pts (correct) + up to 50 bonus pts for speed
- Categories: Science, Geography, History, Pop Culture, Math

---

## Android App Setup

### Prerequisites
- Android Studio (Hedgehog+)
- Min SDK 24 (Android 7.0)
- Target SDK 34

### Configuration

1. Open the `android/` folder in Android Studio
2. Set server IP in `app/src/main/java/com/quizgame/utils/Constants.kt`:

```kotlin
object NetworkConfig {
    var BASE_URL = "http://10.0.2.2:3000"  // Android Emulator (default)
    // var BASE_URL = "http://192.168.1.x:3000"  // Physical device
}
```

### Running on Emulator vs Physical Device

**Emulator:**
- Default `10.0.2.2` maps to host machine's `localhost`
- Just start the server and run the app — it works out of the box

**Physical Device:**
- Find your computer's local IP: `ifconfig` / `ipconfig`
- Update `BASE_URL` to `http://YOUR_IP:3000`
- Make sure both devices are on the same WiFi network

### App Screens

1. **Home** — Enter username, create or join a room
2. **Waiting Room** — See room code, player list, host starts game
3. **Game** — Answer questions with countdown timer, see results
4. **Results** — Final leaderboard, play again, share results

### Libraries Used
- Socket.IO Client Java — Real-time communication
- Retrofit2 + OkHttp3 — REST API calls
- Gson — JSON parsing
- ViewModel + LiveData — MVVM architecture
- ViewBinding — Type-safe view access
- Coroutines — Async operations

---

## How to Play

1. Start the server: `cd server && npm install && node index.js`
2. Player 1 opens the app → enters name → taps "Create Room"
3. Share the 6-digit room code with friends
4. Other players → enter name → tap "Join Room" → enter code
5. Host taps "Start Game"
6. Answer 10 trivia questions — fastest correct answers score most!
7. See the final leaderboard and share results

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Node.js, Express, Socket.IO |
| Database | SQLite (better-sqlite3) |
| Android | Kotlin, MVVM, ViewBinding |
| Networking | Retrofit2, OkHttp3, Socket.IO Client |
| Real-time | WebSocket via Socket.IO |

## License
MIT
