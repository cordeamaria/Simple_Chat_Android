package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AddFriendActivity extends AppCompatActivity {

    Button backBtn, searchBtn;
    EditText searchFriendInput;
    ListView friendListView;
    ArrayAdapter<String> adapter;
    ArrayList<String> friendNames = new ArrayList<>();
    ArrayList<Integer> friendIds = new ArrayList<>();
    String currentUsername;
    String currentEmail;

    int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        currentUserId = intent.getIntExtra("CURRENT_USER_ID", -1);
        currentUsername = intent.getStringExtra("CURRENT_USERNAME");
        currentEmail = intent.getStringExtra("CURRENT_EMAIL");

        if (currentUserId == -1) {
            Toast.makeText(this, "Eroare: utilizatorul nu este logat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_add_friend);

        searchFriendInput = findViewById(R.id.searchFriendInput);
        searchBtn = findViewById(R.id.searchBtn);
        backBtn = findViewById(R.id.backBtn);
        friendListView = findViewById(R.id.friendList);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendNames);
        friendListView.setAdapter(adapter);

        backBtn.setOnClickListener(view -> launchYourProfile());
        searchBtn.setOnClickListener(view -> searchFriend());

        friendListView.setOnItemClickListener((adapterView, view, position, id) -> {
            int receiverId = friendIds.get(position);
            sendFriendRequest(currentUserId, receiverId);
        });
    }

    private void launchYourProfile() {
        Intent intent = new Intent(this, YourProfile.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("USERNAME", currentUsername);
        intent.putExtra("EMAIL", currentEmail);
        startActivity(intent);

    }

    private void searchFriend() {
        String query = searchFriendInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a name or email", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/users_search?q=" + query + "&current_user_id=" + currentUserId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONArray jsonArray = new JSONArray(result.toString());

                    friendNames.clear();
                    friendIds.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject user = jsonArray.getJSONObject(i);
                        int id = user.getInt("id");
                        String username = user.getString("username");
                        String email = user.getString("email");
                        friendNames.add(username + " (" + email + ")");
                        friendIds.add(id);
                    }

                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Search failed!", Toast.LENGTH_SHORT).show());
                }
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendFriendRequest(int senderId, int receiverId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/friend_requests");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("sender_id", senderId);
                json.put("receiver_id", receiverId);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
