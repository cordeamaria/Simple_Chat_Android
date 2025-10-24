const express = require('express'); //framework pt server web
const mysql = require('mysql2');    // conexiunea la baza de date
const bodyParser = require('body-parser'); //pt a citi datele req (json )
const cors = require('cors');   //permite accesul de pe android studio(alte domenii)
const http = require('http'); 
const { Server } = require('socket.io'); 

const app = express();  // serverul
app.use(bodyParser.json());
app.use(cors());

// http (request-response)
// socket.io (comunicare in timp real între server si client)



function formatDateToMySQL(date) {
  return date.toISOString().slice(0, 19).replace('T', ' ');
}


// ceare http server
const server = http.createServer(app);

// initializare socket.io si atasare de http srever
const io = new Server(server, {
  cors: {
    origin: "*", // pt simplitate
    methods: ["GET", "POST"]
  }
});

// conexiune la baza de date
//
const pool = mysql.createPool({    //grup de conexiuni – folosim pentru a rula query-uri SQL
  host: 'localhost',
  user: 'root',
  password: 'parola',
  database: 'android'
});

// rute API

// RUTA DE LOGIN
//
app.post('/login', (req, res) => {
  const { email, username, password } = req.body;

  pool.query(
    'SELECT id FROM users WHERE email = ? AND username = ? AND password = ?', 
    [email, username, password],
    (err, results) => {
      if (err) {
        console.error('Eroare:', err);
        return res.status(500).json({ error: 'Database error' });
      }

      if (results.length > 0) { //daca am gasit userul
        const user = results[0]; 
        res.json({ status: 'SUCCESS', userId: user.id, username: user.username, email: user.email }); // returnam la aplicatie user id si restul datelor
      } else {
        res.json({ status: 'FAIL' });
      }
    }
  );
});


//RUTA DE SIGN UP
//
app.post('/signup', (req, res) => {
  const { email, username, password } = req.body;

  // Verificam dacă exista deja userul
  pool.query(
    'SELECT * FROM users WHERE email = ?',
    [email],
    (err, results) => {
      if (err) {
        console.error('Eroare:', err);
        return res.status(500).json({ error: 'Database error' });
      }

      if (results.length > 0) {
        // Daca exista deja contul
        res.json({ status: 'FAIL', message: 'An account already exists with this email' });
      } else {
        // Daca nu exista, il inseram in baza de date
        pool.query(
          'INSERT INTO users (email, username, password) VALUES (?, ?, ?)',
          [email, username, password],
          (err2, results2) => {
            if (err2) {
              console.error('Eroare la inserare:', err2);
              return res.status(500).json({ error: 'Database error' });
            }
            // retrunam id ul
            res.json({ status: 'SUCCESS', message: 'Account created successfully', userId: results2.insertId });
          }
        );
      }
    }
  );
});



// RUTA DE USERS SEARCH
// 
app.get('/users_search', (req, res) => {
  const query = req.query.q;  //a parametrul q trimis in url- ex: /users_search?q=ana
  const currentUserId = parseInt(req.query.current_user_id, 10); //parseInt are doua argumente, sirul care e convertit si baza in care se converteste

  if (!query || !currentUserId) {
 // cunsidera 0 sau nan ca fals dar id ul incepe de la 1 so it s ok
    return res.status(400).json({ error: 'Search query and user ID are required' });
  }

  const searchQuery = `%${query}%`; // se construieste un wild card % extinde cautarea sa nu fie exacta, ci conține sirul cautat

  const sql = `
    SELECT id, username, email FROM users
    WHERE (username LIKE ? OR email LIKE ?)
      AND id != ? -- exclude pe sine
      AND id NOT IN (
        SELECT CASE 
                 WHEN user1_id = ? THEN user2_id 
                 ELSE user1_id 
               END AS friend_id
        FROM friend_relationships
        WHERE (user1_id = ? OR user2_id = ?)
          AND status IN ('pending', 'accepted', 'rejected')
      )
  `;

  //maping
const values = [
  searchQuery,   // 1 -> username LIKE ?
  searchQuery,   // 2 -> email LIKE ?
  currentUserId, // 3 -> id != ?
  currentUserId, // 4 -> CASE WHEN user1_id = ?
  currentUserId, // 5 -> WHERE user1_id = ?
  currentUserId  // 6 -> OR user2_id = ?
];

  pool.query(sql, values, (err, results) => {
    if (err) {
      console.error('Error searching users:', err);
      return res.status(500).json({ error: 'Database error' });
    }
    res.json(results);
  });
});

// RUTA FRIEND REQUEST
// 
app.post('/friend_requests', (req, res) => {
  const { sender_id, receiver_id } = req.body;
  if (!sender_id || !receiver_id) {
    return res.status(400).json({ error: 'Sender and receiver IDs are required' });
  }

  // asigura ca user1_id < user2_id 
  const user1 = Math.min(sender_id, receiver_id);
  const user2 = Math.max(sender_id, receiver_id);
  const actionUser = sender_id; // user care a facut cererea

  pool.query(
    'INSERT INTO friend_relationships (user1_id, user2_id, status, action_user_id) VALUES (?, ?, ?, ?)',
    [user1, user2, 'pending', actionUser],
    (err, results) => {
      if (err) {
        console.error('Error sending friend request:', err);
        // Check for unique constraint violation (duplicate request)
        if (err.code === 'ER_DUP_ENTRY') {
          return res.status(409).json({ status: 'FAIL', message: 'Friend request already sent or users are already friends.' });
        }
        return res.status(500).json({ error: 'Database error' });
      }
      res.json({ status: 'SUCCESS', message: 'Friend request sent.' });

      //Socket.IO:notifica clientul ca a primit o crere de prietenie 
      io.to(receiver_id.toString()).emit('new_friend_request', { senderId: sender_id, senderUsername: req.body.sender_username }); // Emit to receiver's room
    }
  );
});

// RUTA FRIENDS LIST
// 
app.get('/friends/:userId', (req, res) => {
  const userId = req.params.userId;
  pool.query(
    `SELECT u.id, u.username, u.email, fr.status
     FROM friend_relationships fr
     JOIN users u ON (u.id = fr.user1_id OR u.id = fr.user2_id)
     WHERE (fr.user1_id = ? OR fr.user2_id = ?) AND fr.status = 'accepted' AND u.id != ?`,
    [userId, userId, userId],
    (err, results) => {
      if (err) {
        console.error('Error fetching friends:', err);
        return res.status(500).json({ error: 'Database error' });
      }
      res.json(results);
    }
  );
});

// RUTA ACCEPT FRIEND REQUEST
// 
app.post('/friend_requests/accept', (req, res) => {
  const { sender_id, receiver_id } = req.body;

  pool.query(
    `UPDATE friend_relationships
     SET status = 'accepted', action_user_id = ?
     WHERE ((user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?))
     AND status = 'pending'`,
    [receiver_id, sender_id, receiver_id, receiver_id, sender_id],
    (err, results) => {
      if (err) {
        console.error('Error accepting friend request:', err);
        return res.status(500).json({ error: 'Database error' });
      }
      if (results.affectedRows === 0) {
        return res.status(404).json({ status: 'FAIL', message: 'Friend request not found or already handled.' });
      }

      res.json({ status: 'SUCCESS', message: 'Friend request accepted.' });

      // Notificare Socket.IO
      pool.query(
        `SELECT id, user1_id, user2_id FROM friend_relationships
         WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)`,
        [sender_id, receiver_id, receiver_id, sender_id],
        (err, rows) => {
          if (!err && rows.length > 0) {
            const { user1_id, user2_id } = rows[0];
            io.to(user1_id.toString()).emit('friend_accepted', { friendId: user2_id });
            io.to(user2_id.toString()).emit('friend_accepted', { friendId: user1_id });
          }
        }
      );
    }
  );
});

//RUTA REJECT FRIEND REQUEST
// 
app.post('/friend_requests/reject', (req, res) => {
  const { sender_id, receiver_id } = req.body;

  pool.query(
    `UPDATE friend_relationships
     SET status = 'rejected', action_user_id = ?
     WHERE ((user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?))
     AND status = 'pending'`,
    [receiver_id, sender_id, receiver_id, receiver_id, sender_id],
    (err, results) => {
      if (err) {
        console.error('Error rejecting friend request:', err);
        return res.status(500).json({ error: 'Database error' });
      }
      if (results.affectedRows === 0) {
        return res.status(404).json({ status: 'FAIL', message: 'Friend request not found or already handled.' });
      }

      res.json({ status: 'SUCCESS', message: 'Friend request rejected.' });

      // notificare Socket.IO
      pool.query(
        `SELECT id, user1_id, user2_id FROM friend_relationships
         WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)`,
        [sender_id, receiver_id, receiver_id, sender_id],
        (err, rows) => {
          if (!err && rows.length > 0) {
            const { user1_id, user2_id } = rows[0];
            io.to(user1_id.toString()).emit('friend_accepted', { friendId: user2_id });
            io.to(user2_id.toString()).emit('friend_accepted', { friendId: user1_id });
          }
        }
      );
    }
  );
});


// RUTA USER'S FRIENDS REQUESTS
// 
app.get('/friends_requests/:userId', (req, res) => {
  const userId = req.params.userId;
  pool.query(
    `SELECT u.id, u.username, u.email, fr.status
     FROM friend_relationships fr
     JOIN users u ON (u.id = fr.user1_id OR u.id = fr.user2_id)
     WHERE (fr.user1_id = ? OR fr.user2_id = ?) AND fr.status = 'pending' AND u.id != ? AND fr.action_user_id !=?`,
    [userId, userId, userId, userId],
    (err, results) => {
      if (err) {
        console.error('Error fetching friends:', err);
        return res.status(500).json({ error: 'Database error' });
      }
      res.json(results);
    }
  );
});


// RUTA MESSAGES
//
app.get('/messages', (req, res) => {
  const user1 = parseInt(req.query.user1);
  const user2 = parseInt(req.query.user2);

  if (!user1 || !user2) {
    return res.status(400).json({ error: 'User IDs are required' });
  }

  pool.query(
    `SELECT sender_id, receiver_id, message, sent_at FROM messages
     WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
     ORDER BY sent_at ASC`,
    [user1, user2, user2, user1],
    (err, results) => {
      if (err) {
        console.error('Error fetching messages:', err);
        return res.status(500).json({ error: 'Database error' });
      }
      res.json(results);
    }
  );
});



//Socket.IO Connection Handling 
// 
io.on('connection', (socket) => {
  console.log(`User connected: ${socket.id}`);

  // cand user se logheaza, intra intr-o "caemra" cu numita dupa userId-ul lor
  // asta permite trimiterea mesajelor private
  socket.on('register_user_socket', (userId) => {
    socket.join(userId.toString()); // intra in camera
    console.log(`Socket ${socket.id} joined room for user ${userId}`);
  });

  // se coupa de mesaje
 socket.on('chat_message', async (data) => {
  const { senderId, receiverId, messageContent } = data;
  const currentDateTime = formatDateToMySQL(new Date()); 

  console.log(`Message from ${senderId} to ${receiverId}: ${messageContent}`);

  try {
    await pool.promise().query(
      'INSERT INTO messages (sender_id, receiver_id, message, sent_at) VALUES (?, ?, ?, ?)',
      [senderId, receiverId, messageContent, currentDateTime]
    );

    // 
    io.to(receiverId.toString()).emit('new_message', { ...data, timestamp: currentDateTime });
    io.to(senderId.toString()).emit('new_message', { ...data, timestamp: currentDateTime });

  } catch (err) {
    console.error('Error saving message:', err);
    socket.emit('message_error', { message: 'Failed to send message.' });
  }
});


  // deconectare
  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.id}`);
    // Clean up any userId-to-socketId mappings if you stored them
  });
});

// pornim serverul
// Change app.listen to server.listen to include Socket.IO
server.listen(3000, "0.0.0.0", () => {
  console.log('Server running on http://0.0.0.0:3000');
});


//rulez cu node server.js
