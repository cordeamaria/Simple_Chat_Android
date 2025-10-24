package com.example.chat_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException; // Make sure this is imported
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogIn extends AppCompatActivity {

    EditText usernameInput, passwordInput, emailInput;
    Button loginBtn,backBtn;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@mail.com$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailInput = findViewById(R.id.emailInput);
        loginBtn = findViewById(R.id.loginBtn);
        backBtn = findViewById(R.id.backBtn);

        loginBtn.setOnClickListener(view -> login());
        backBtn.setOnClickListener(view -> lounchMain());
    }
    private void lounchMain() {
        startActivity(new Intent(this, Main.class));

    }
    private void login() {
        String email = "maria@mail.com";
        String username = "maria";
        String password = "parola123";
//        String email = emailInput.getText().toString().trim(); // trim pentru a elimina spatiile goale
//        String username = usernameInput.getText().toString().trim();
//        String password = passwordInput.getText().toString().trim();
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completati toate campurile!", Toast.LENGTH_SHORT).show();
            return;
        }
//        if (!PASSWORD_PATTERN.matcher(password).matches()) {
//            Toast.makeText(this, "Parola trebuie să conțină minim 8 caractere, o literă mare, o literă mică, un număr și un caracter special!", Toast.LENGTH_SHORT).show();
//            return;
//        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.matches()) {
            Toast.makeText(this, "Email invalid!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/login"); // IP corect pt localhost
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("username", username);
                jsonParam.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes());
                os.flush();
                os.close();

                // citește răspunsul
                java.io.InputStream inputStream = conn.getInputStream();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                // transformă în JSON
                JSONObject responseJson = new JSONObject(result.toString());
                String status = responseJson.getString("status");

                runOnUiThread(() -> {
                    if (status.equals("SUCCESS")) {
                        int userId=-1;
                        try {
                            userId = responseJson.getInt("userId");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        Toast.makeText(this, "Login OK!", Toast.LENGTH_SHORT).show();
                       Intent intent= new Intent(this, YourProfile.class);
                       intent.putExtra("CURRENT_USER_ID", userId);
                       intent.putExtra("USERNAME", username);
                       intent.putExtra("EMAIL", email);
                       startActivity(intent);

                    } else {
                        Toast.makeText(this, "Date greșite!", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

}
