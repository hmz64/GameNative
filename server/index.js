const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const Database = require('better-sqlite3');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const path = require('path');

// ─── App Setup ───────────────────────────────────────────────
const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] }
});

app.use(cors());
app.use(express.json());

const PORT = 3000;

// ─── Database Setup ──────────────────────────────────────────
const db = new Database(path.join(__dirname, 'quiz.db'));
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS rooms (
    id TEXT PRIMARY KEY,
    room_code TEXT UNIQUE NOT NULL,
    host_id TEXT NOT NULL,
    status TEXT DEFAULT 'waiting',
    current_question INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS players (
    id TEXT PRIMARY KEY,
    room_id TEXT NOT NULL,
    username TEXT NOT NULL,
    score INTEGER DEFAULT 0,
    is_connected INTEGER DEFAULT 1,
    FOREIGN KEY (room_id) REFERENCES rooms(id)
  );
  CREATE TABLE IF NOT EXISTS answers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id TEXT NOT NULL,
    room_id TEXT NOT NULL,
    question_index INTEGER NOT NULL,
    answer_index INTEGER NOT NULL,
    is_correct INTEGER NOT NULL,
    time_taken REAL NOT NULL
  );
`);

// ─── Hardcoded Questions (20) ────────────────────────────────
const ALL_QUESTIONS = [
  { id: 1, category: "Science", text: "What planet is known as the Red Planet?", options: ["Venus", "Jupiter", "Mars", "Saturn"], correctIndex: 2 },
  { id: 2, category: "Geography", text: "What is the largest continent by area?", options: ["Africa", "Asia", "Europe", "North America"], correctIndex: 1 },
  { id: 3, category: "History", text: "In what year did World War II end?", options: ["1943", "1944", "1945", "1946"], correctIndex: 2 },
  { id: 4, category: "Pop Culture", text: "Who directed the movie 'Inception'?", options: ["Steven Spielberg", "Christopher Nolan", "James Cameron", "Martin Scorsese"], correctIndex: 1 },
  { id: 5, category: "Math", text: "What is the square root of 144?", options: ["10", "11", "12", "13"], correctIndex: 2 },
  { id: 6, category: "Science", text: "What is the chemical symbol for gold?", options: ["Go", "Gd", "Au", "Ag"], correctIndex: 2 },
  { id: 7, category: "Geography", text: "Which country has the most natural lakes?", options: ["USA", "Canada", "Russia", "Brazil"], correctIndex: 1 },
  { id: 8, category: "History", text: "Who was the first President of the United States?", options: ["Thomas Jefferson", "George Washington", "John Adams", "Benjamin Franklin"], correctIndex: 1 },
  { id: 9, category: "Pop Culture", text: "What band released the album 'Abbey Road'?", options: ["The Rolling Stones", "The Who", "Led Zeppelin", "The Beatles"], correctIndex: 3 },
  { id: 10, category: "Math", text: "What is 17 × 6?", options: ["96", "102", "108", "112"], correctIndex: 1 },
  { id: 11, category: "Science", text: "What gas do plants absorb from the atmosphere?", options: ["Oxygen", "Nitrogen", "Carbon Dioxide", "Hydrogen"], correctIndex: 2 },
  { id: 12, category: "Geography", text: "What is the capital of Australia?", options: ["Sydney", "Melbourne", "Canberra", "Perth"], correctIndex: 2 },
  { id: 13, category: "History", text: "The ancient city of Rome was built on how many hills?", options: ["5", "6", "7", "8"], correctIndex: 2 },
  { id: 14, category: "Pop Culture", text: "What is the highest-grossing film of all time (unadjusted)?", options: ["Avengers: Endgame", "Avatar", "Titanic", "Star Wars: The Force Awakens"], correctIndex: 1 },
  { id: 15, category: "Math", text: "What is the value of Pi rounded to two decimal places?", options: ["3.12", "3.14", "3.16", "3.18"], correctIndex: 1 },
  { id: 16, category: "Science", text: "What is the hardest natural substance on Earth?", options: ["Gold", "Iron", "Diamond", "Platinum"], correctIndex: 2 },
  { id: 17, category: "Geography", text: "Which river is the longest in the world?", options: ["Amazon", "Nile", "Yangtze", "Mississippi"], correctIndex: 1 },
  { id: 18, category: "History", text: "In what year did the Titanic sink?", options: ["1910", "1911", "1912", "1913"], correctIndex: 2 },
  { id: 19, category: "Pop Culture", text: "Who painted the Mona Lisa?", options: ["Michelangelo", "Raphael", "Leonardo da Vinci", "Donatello"], correctIndex: 2 },
  { id: 20, category: "Math", text: "What is 2 to the power of 10?", options: ["512", "1024", "2048", "4096"], correctIndex: 1 }
];

// ─── In-memory game state ────────────────────────────────────
const gameRooms = {}; // { roomCode: { questions, timers, answeredPlayers, currentQuestion } }

function generateRoomCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code = '';
  for (let i = 0; i < 6; i++) code += chars.charAt(Math.floor(Math.random() * chars.length));
  return code;
}

function getRandomQuestions(count = 10) {
  const shuffled = [...ALL_QUESTIONS].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, count);
}

function getPlayersInRoom(roomId) {
  return db.prepare('SELECT id, username, score, is_connected FROM players WHERE room_id = ?').all(roomId);
}

function getRoomByCode(code) {
  return db.prepare('SELECT * FROM rooms WHERE room_code = ?').get(code);
}

// ─── REST API ────────────────────────────────────────────────

// POST /api/room/create
app.post('/api/room/create', (req, res) => {
  const { hostName } = req.body;
  if (!hostName || !hostName.trim()) {
    return res.status(400).json({ error: 'hostName is required' });
  }

  let roomCode;
  do {
    roomCode = generateRoomCode();
  } while (getRoomByCode(roomCode));

  const roomId = uuidv4();
  const playerId = uuidv4();

  db.prepare('INSERT INTO rooms (id, room_code, host_id, status) VALUES (?, ?, ?, ?)').run(roomId, roomCode, playerId, 'waiting');
  db.prepare('INSERT INTO players (id, room_id, username, score) VALUES (?, ?, ?, 0)').run(playerId, roomId, hostName.trim());

  console.log(`[ROOM CREATED] Code: ${roomCode}, Host: ${hostName}, HostID: ${playerId}`);
  res.json({ roomCode, playerId });
});

// POST /api/room/join
app.post('/api/room/join', (req, res) => {
  const { roomCode, username } = req.body;
  if (!roomCode || !username || !username.trim()) {
    return res.status(400).json({ error: 'roomCode and username are required' });
  }

  const room = getRoomByCode(roomCode.toUpperCase());
  if (!room) {
    return res.status(404).json({ error: 'Room not found' });
  }
  if (room.status !== 'waiting') {
    return res.status(400).json({ error: 'Game already started' });
  }

  const playerId = uuidv4();
  db.prepare('INSERT INTO players (id, room_id, username, score) VALUES (?, ?, ?, 0)').run(playerId, room.id, username.trim());

  const players = getPlayersInRoom(room.id);
  console.log(`[PLAYER JOINED] ${username} → Room: ${roomCode}`);
  res.json({ playerId, roomCode: room.room_code, players });
});

// GET /api/questions
app.get('/api/questions', (req, res) => {
  res.json({ questions: ALL_QUESTIONS });
});

// GET /api/room/:code (helper for debugging)
app.get('/api/room/:code', (req, res) => {
  const room = getRoomByCode(req.params.code.toUpperCase());
  if (!room) return res.status(404).json({ error: 'Room not found' });
  const players = getPlayersInRoom(room.id);
  res.json({ ...room, players });
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', uptime: process.uptime() });
});

// ─── Socket.IO ───────────────────────────────────────────────

io.on('connection', (socket) => {
  console.log(`[SOCKET] Connected: ${socket.id}`);

  // join_room
  socket.on('join_room', ({ roomCode, playerId, username }) => {
    if (!roomCode || !playerId) {
      socket.emit('error', { message: 'roomCode and playerId required' });
      return;
    }

    const room = getRoomByCode(roomCode.toUpperCase());
    if (!room) {
      socket.emit('error', { message: 'Room not found' });
      return;
    }

    socket.join(roomCode.toUpperCase());
    socket.data = { roomCode: roomCode.toUpperCase(), playerId, roomId: room.id };

    // Mark connected
    db.prepare('UPDATE players SET is_connected = 1 WHERE id = ?').run(playerId);

    const players = getPlayersInRoom(room.id);
    io.to(roomCode.toUpperCase()).emit('room_updated', { players });
    console.log(`[SOCKET] ${username} joined room ${roomCode}`);
  });

  // start_game
  socket.on('start_game', ({ roomCode, playerId }) => {
    const room = getRoomByCode(roomCode.toUpperCase());
    if (!room) {
      socket.emit('error', { message: 'Room not found' });
      return;
    }
    if (room.host_id !== playerId) {
      socket.emit('error', { message: 'Only the host can start the game' });
      return;
    }
    if (room.status !== 'waiting') {
      socket.emit('error', { message: 'Game already started' });
      return;
    }

    const questions = getRandomQuestions(10);
    db.prepare('UPDATE rooms SET status = ?, current_question = 0 WHERE id = ?').run('playing', room.id);

    gameRooms[roomCode.toUpperCase()] = {
      roomId: room.id,
      questions,
      currentQuestion: 0,
      answeredPlayers: new Set(),
      timer: null,
      totalPlayers: getPlayersInRoom(room.id).length
    };

    io.to(roomCode.toUpperCase()).emit('game_started', { totalQuestions: questions.length });
    console.log(`[GAME] Started in room ${roomCode} with ${questions.length} questions`);

    // Send first question after a short delay
    setTimeout(() => sendQuestion(roomCode.toUpperCase()), 1000);
  });

  // submit_answer
  socket.on('submit_answer', ({ roomCode, playerId, questionIndex, answerIndex, timeLeft }) => {
    const code = roomCode.toUpperCase();
    const gameRoom = gameRooms[code];
    if (!gameRoom) {
      socket.emit('error', { message: 'Game not found' });
      return;
    }

    if (gameRoom.answeredPlayers.has(playerId)) {
      return; // Already answered
    }

    const question = gameRoom.questions[questionIndex];
    if (!question) {
      socket.emit('error', { message: 'Invalid question index' });
      return;
    }

    const isCorrect = answerIndex === question.correctIndex;
    const timeLimit = 15;
    const timeTaken = timeLimit - (timeLeft || 0);
    let scoreGained = 0;

    if (isCorrect) {
      scoreGained = 100 + Math.round((timeLeft / timeLimit) * 50);
      db.prepare('UPDATE players SET score = score + ? WHERE id = ?').run(scoreGained, playerId);
    }

    db.prepare('INSERT INTO answers (player_id, room_id, question_index, answer_index, is_correct, time_taken) VALUES (?, ?, ?, ?, ?, ?)').run(
      playerId, gameRoom.roomId, questionIndex, answerIndex, isCorrect ? 1 : 0, timeTaken
    );

    gameRoom.answeredPlayers.add(playerId);
    console.log(`[ANSWER] Player ${playerId} answered Q${questionIndex}: ${isCorrect ? 'CORRECT' : 'WRONG'} (+${scoreGained})`);

    // Check if all players answered
    if (gameRoom.answeredPlayers.size >= gameRoom.totalPlayers) {
      clearTimeout(gameRoom.timer);
      processQuestionEnd(code);
    }
  });

  // disconnect
  socket.on('disconnect', () => {
    console.log(`[SOCKET] Disconnected: ${socket.id}`);
    if (socket.data && socket.data.playerId) {
      db.prepare('UPDATE players SET is_connected = 0 WHERE id = ?').run(socket.data.playerId);

      if (socket.data.roomCode) {
        const room = getRoomByCode(socket.data.roomCode);
        if (room) {
          const players = getPlayersInRoom(room.id);
          io.to(socket.data.roomCode).emit('room_updated', { players });

          // If host disconnected during game, notify
          if (room.host_id === socket.data.playerId && room.status === 'playing') {
            io.to(socket.data.roomCode).emit('error', { message: 'Host disconnected. Game ended.' });
          }
        }
      }
    }
  });
});

// ─── Game Flow Functions ─────────────────────────────────────

function sendQuestion(roomCode) {
  const gameRoom = gameRooms[roomCode];
  if (!gameRoom) return;

  const qIndex = gameRoom.currentQuestion;
  if (qIndex >= gameRoom.questions.length) {
    endGame(roomCode);
    return;
  }

  const question = gameRoom.questions[qIndex];
  gameRoom.answeredPlayers = new Set();

  // Update connected player count
  gameRoom.totalPlayers = getPlayersInRoom(gameRoom.roomId).filter(p => p.is_connected).length;

  io.to(roomCode).emit('new_question', {
    index: qIndex,
    text: question.text,
    options: question.options,
    timeLimit: 15
  });
  console.log(`[QUESTION] Room ${roomCode}: Q${qIndex + 1}/${gameRoom.questions.length} - "${question.text}"`);

  // Start 15-second timer
  gameRoom.timer = setTimeout(() => {
    console.log(`[TIMER] Time's up for Q${qIndex} in room ${roomCode}`);
    processQuestionEnd(roomCode);
  }, 15000);
}

function processQuestionEnd(roomCode) {
  const gameRoom = gameRooms[roomCode];
  if (!gameRoom) return;

  const question = gameRoom.questions[gameRoom.currentQuestion];
  const players = getPlayersInRoom(gameRoom.roomId);

  // Build scores object
  const scores = {};
  players.forEach(p => { scores[p.username] = p.score; });

  // Send answer_result to all
  io.to(roomCode).emit('answer_result', {
    correct: true, // individual correctness handled client-side
    correctIndex: question.correctIndex,
    scores
  });

  // Send leaderboard
  const rankings = players
    .sort((a, b) => b.score - a.score)
    .map((p, i) => ({ username: p.username, score: p.score, rank: i + 1 }));

  setTimeout(() => {
    io.to(roomCode).emit('leaderboard', { rankings });
    console.log(`[LEADERBOARD] Room ${roomCode}:`, rankings.map(r => `${r.rank}. ${r.username}: ${r.score}`).join(', '));

    // Move to next question after 3 seconds
    gameRoom.currentQuestion++;
    db.prepare('UPDATE rooms SET current_question = ? WHERE room_code = ?').run(gameRoom.currentQuestion, roomCode);

    setTimeout(() => sendQuestion(roomCode), 3000);
  }, 1000);
}

function endGame(roomCode) {
  const gameRoom = gameRooms[roomCode];
  if (!gameRoom) return;

  db.prepare('UPDATE rooms SET status = ? WHERE room_code = ?').run('finished', roomCode);

  const players = getPlayersInRoom(gameRoom.roomId);
  const finalRankings = players
    .sort((a, b) => b.score - a.score)
    .map((p, i) => ({ username: p.username, score: p.score, rank: i + 1 }));

  io.to(roomCode).emit('game_over', { finalRankings });
  console.log(`[GAME OVER] Room ${roomCode}. Winner: ${finalRankings[0]?.username} with ${finalRankings[0]?.score} points`);

  // Clean up
  delete gameRooms[roomCode];
}

// ─── Start Server ────────────────────────────────────────────
server.listen(PORT, '0.0.0.0', () => {
  console.log(`🎮 Quiz Game Server running on http://0.0.0.0:${PORT}`);
  console.log(`   REST API: http://localhost:${PORT}/api/`);
  console.log(`   Socket.IO: ws://localhost:${PORT}`);
});
