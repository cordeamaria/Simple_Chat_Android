package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FriendsListActivity extends AppCompatActivity {

    private ListView friendsListView;
    private TextView noFriendsMessage;
    private ArrayList<String> friendUsernames = new ArrayList<>();
    private ArrayList<Integer> friendIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private int currentUserId;
    private String currentUsername;
    private String currentEmail;

    Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        friendsListView = findViewById(R.id.friendsListView);
        noFriendsMessage = findViewById(R.id.noFriendsMessage);
        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(view -> launchYourProfile());

        Intent intent = getIntent();
        if (intent != null) {
            currentUserId = intent.getIntExtra("CURRENT_USER_ID",-1);
            currentEmail = intent.getStringExtra("CURRENT_EMAIL");
            currentUsername = intent.getStringExtra("CURRENT_USERNAME");

            if (currentUserId == -1) {
                Toast.makeText(this, "Error: User ID not found.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Error: Intent is null.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendUsernames);
        friendsListView.setAdapter(adapter);

        fetchFriendsList();

        friendsListView.setOnItemClickListener((parent, view, position, id) -> {
            int friendId = friendIds.get(position);
            String friendUsername = friendUsernames.get(position);

            Intent chatIntent = new Intent(FriendsListActivity.this, Chat.class);
            chatIntent.putExtra("CURRENT_USER_ID", currentUserId);
            chatIntent.putExtra("FRIEND_ID", friendId);
            chatIntent.putExtra("FRIEND_USERNAME", friendUsername);
            chatIntent.putExtra("CURRENT_USERNAME", currentUsername);
            chatIntent.putExtra("CURRENT_EMAIL", currentEmail);
            startActivity(chatIntent);
        });
    }

    private void launchYourProfile() {
        Intent intent = new Intent(this, YourProfile.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("USERNAME", currentUsername);
        intent.putExtra("EMAIL", currentEmail);
        startActivity(intent);
    }

    private void fetchFriendsList() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://10.0.2.2:3000/friends/" + currentUserId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());

                    friendUsernames.clear();
                    friendIds.clear();

                    if (jsonArray.length() == 0) {
                        runOnUiThread(() -> noFriendsMessage.setVisibility(TextView.VISIBLE));
                    } else {
                        runOnUiThread(() -> noFriendsMessage.setVisibility(TextView.GONE));
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject friend = jsonArray.getJSONObject(i);
                            String username = friend.getString("username");
                            int friendId = friend.getInt("id");

                            friendUsernames.add(username);
                            friendIds.add(friendId);
                        }
                    }

                    runOnUiThread(() -> adapter.notifyDataSetChanged());

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error fetching friends: " + responseCode, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
