package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity {

    Button loginBtn, singInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginBtn = findViewById(R.id.loginBtn1);
        loginBtn.setOnClickListener(view -> lounchLogin());
        singInBtn = findViewById(R.id.signInBtn1);
        singInBtn.setOnClickListener(view -> lounchSingUp());
    }

    private void lounchLogin() {
        startActivity(new Intent(this, LogIn.class));

    }

    private void lounchSingUp() {
        startActivity(new Intent(this, SignUp.class));
    }
}