package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


public class YourProfile extends AppCompatActivity {

    private TextView usernameDisplayTextView;
    private TextView emailDisplayTextView;
    private Button chatWithFriendsButton;
    private Button addFriendsButton;
    private Button friendsRequestButton;

    // Variables to store user data
    private String loggedInUsername;
    private String loggedInEmail;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_profile);

        usernameDisplayTextView = findViewById(R.id.usernameDisplayTextView);
        emailDisplayTextView = findViewById(R.id.emailDisplayTextView);
        chatWithFriendsButton = findViewById(R.id.chatWithFriendsButton);
        addFriendsButton = findViewById(R.id.addFriendsButton);
        friendsRequestButton = findViewById(R.id.friendsRequestButton);

        Intent intent = getIntent();
        if (intent != null) {
            loggedInUsername = intent.getStringExtra("USERNAME");
            loggedInEmail = intent.getStringExtra("EMAIL");
            currentUserId = intent.getIntExtra("CURRENT_USER_ID", -1);

            if (loggedInUsername != null) {
                usernameDisplayTextView.setText(loggedInUsername);
            }
            if (loggedInEmail != null) {
                emailDisplayTextView.setText(loggedInEmail);
            }

        }

        chatWithFriendsButton.setOnClickListener(view -> launchFriendsListActivity());
        addFriendsButton.setOnClickListener(view -> launchAddFriendActivity());
        friendsRequestButton.setOnClickListener(view -> launchFriendsRequestActivity());
    }

    private void launchFriendsListActivity() {
        Intent intent = new Intent(this, FriendsListActivity.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("CURRENT_USERNAME", loggedInUsername);
        intent.putExtra("CURRENT_EMAIL", loggedInEmail);
        startActivity(intent);
    }

    private void launchAddFriendActivity() {
        Intent intent = new Intent(this, AddFriendActivity.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("CURRENT_USERNAME", loggedInUsername);
        intent.putExtra("CURRENT_EMAIL", loggedInEmail);
        startActivity(intent);
    }
    private void launchFriendsRequestActivity() {
        Intent intent = new Intent(this, FriendsRequest.class);
        intent.putExtra("CURRENT_USER_ID", currentUserId);
        intent.putExtra("CURRENT_USERNAME", loggedInUsername);
        intent.putExtra("CURRENT_EMAIL", loggedInEmail);
        startActivity(intent);
    }


}