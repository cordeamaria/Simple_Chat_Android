package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import io.socket.client.IO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;



public class Chat extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private Button btnSend, backBtn;
    private ChatAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<>();

    private io.socket.client.Socket socket;

    private int currentUserId;
    private int friendId;
    private String friendUsername;
    private String currentEmail;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        getIntentData();
        setupRecyclerView();
        setupSocketConnection();
        setupSendButton();
        loadOldMessages();
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btSend);
       backBtn = findViewById(R.id.backBtn);
       backBtn.setOnClickListener(view -> launchFriendsList());
    }

    private void getIntentData() {
        Intent intent = getIntent();
        currentUserId = intent.getIntExtra("CURRENT_USER_ID", -1);
        friendId = intent.getIntExtra("FRIEND_ID", -1);
        friendUsername = intent.getStringExtra("FRIEND_USERNAME");
        currentUsername = intent.getStringExtra("CURRENT_USERNAME");
        currentEmail = intent.getStringExtra("CURRENT_EMAIL");
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);
    }

    private void setupSocketConnection() {
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            socket = IO.socket("http://10.0.2.2:3000", options);
            socket.connect();

            socket.emit("register_user_socket", currentUserId);

            socket.on("new_message", args -> runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                try {
                    int senderId = data.getInt("senderId");
                    int receiverId = data.getInt("receiverId");
                    String content = data.getString("messageContent");
                    String sentAt = data.has("sentAt") ? data.getString("sentAt") : "";

                    if ((senderId == currentUserId && receiverId == friendId) ||
                            (senderId == friendId && receiverId == currentUserId)) {

                        Message message = new Message(senderId, receiverId, content, sentAt);
                        messageList.add(message);

                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String content = editMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                JSONObject message = new JSONObject();
                try {
                    message.put("senderId", currentUserId);
                    message.put("receiverId", friendId);
                    message.put("messageContent", content);

                    // Optional: add timestamp in ISO 8601 format
                    String sentAt = java.time.LocalDateTime.now().toString().replace("T", " ");
                    message.put("sentAt", sentAt);

                    socket.emit("chat_message", message);
                    editMessage.setText("");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void launchFriendsList() {
        Intent intent = new Intent(this, FriendsListActivity.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("CURRENT_USERNAME", currentUsername);
        intent.putExtra("CURRENT_EMAIL", currentEmail);
        startActivity(intent);
    }

    private void loadOldMessages() {
        OkHttpClient client = new OkHttpClient();

        HttpUrl url = HttpUrl.parse("http://10.0.2.2:3000/messages").newBuilder()
                .addQueryParameter("user1", String.valueOf(currentUserId))
                .addQueryParameter("user2", String.valueOf(friendId))
                .build();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                String json = response.body().string();
                try {
                    JSONArray messagesArray = new JSONArray(json);
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject msg = messagesArray.getJSONObject(i);
                        int senderId = msg.getInt("sender_id");
                        int receiverId = msg.getInt("receiver_id");
                        String content = msg.getString("message");
                        String sentAt = msg.has("sent_at") ? msg.getString("sent_at") : "";

                        Message message = new Message(senderId, receiverId, content, sentAt);
                        messageList.add(message);
                    }

                    runOnUiThread(() -> {
                        chatAdapter.notifyDataSetChanged();
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
