package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FriendsRequest extends AppCompatActivity {

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
            int receiverId = friendIds.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Friend Request")
                    .setMessage("Do you want to accept or reject this request?")
                    .setPositiveButton("Accept", (dialog, which) -> {
                        acceptRequest(currentUserId, receiverId);  // call accept function
                    })
                    .setNegativeButton("Reject", (dialog, which) -> {
                        rejectRequest(currentUserId, receiverId);  // call reject function
                    })
                    .setCancelable(true) // allows dismissing by tapping outside
                    .show();
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
                URL url = new URL("http://10.0.2.2:3000/friends_requests/" + currentUserId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // 200 OK
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

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged(); // Update the ListView
                    });

                } else {
                    // Handle non-200 responses
                    runOnUiThread(() -> Toast.makeText(this, "Error fetching friends: " + responseCode, Toast.LENGTH_LONG).show());
                    Log.e("FriendsListActivity", "Server returned non-OK status: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("FriendsListActivity", "Exception in fetching friends: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
    private void acceptRequest(int senderId, int receiverId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/friend_requests/accept");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("sender_id", senderId);   // Cel care a trimis cererea
                json.put("receiver_id", receiverId); // Cel care acceptă

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                        fetchFriendsList();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private void rejectRequest(int senderId, int receiverId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/friend_requests/reject");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("sender_id", senderId);
                jsonParam.put("receiver_id", receiverId);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() ->
                        Toast.makeText(this, "Reject response: " + responseCode, Toast.LENGTH_SHORT).show()
                );

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Eroare reject: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

}
