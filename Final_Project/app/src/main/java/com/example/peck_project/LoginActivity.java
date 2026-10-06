package com.example.peck_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    // UI Input Elements
    private EditText etUsername, etPassword;
    private android.widget.Button btnLogin;
    private android.widget.Button btnCreate;

    // Database Reference Link
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure this matches your login layout file name (e.g., activity_login)
        setContentView(R.layout.activity_main);

        // Initialize the unified database connection
        dbHelper = new DatabaseHelper(this);

        // Map layout element IDs
        etUsername = findViewById(R.id.app_login_user); // Or your email field ID
        etPassword = findViewById(R.id.app_login_pass);
        btnLogin = findViewById(R.id.btn_primary_login);
        btnCreate = findViewById(R.id.btn_create_login);

        // Handle Login verification routing
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Extract input string text values and trim accidental whitespaces
                String username = etUsername.getText().toString().trim(); // Treat username input explicitly as Email
                String password = etPassword.getText().toString().trim();

                // Guard clauses: Check for empty fields before querying the DB
                if (username.isEmpty()) {
                    etUsername.setError("Email cannot be empty");
                    return;
                }
                if (password.isEmpty()) {
                    etPassword.setError("Password cannot be empty");
                    return;
                }

                // Supabase requests run over the web, so move execution off the main UI thread
                new Thread(() -> {
                    try {
                        SupabaseClient client = SupabaseClient.getInstance();
                        String authResult = client.authenticateUser(username, password);

                        runOnUiThread(() -> {
                            if (authResult != null) {
                                // FIXED: Split the combined response token data strings cleanly
                                String[] authData = authResult.split(",");
                                String jwtToken = authData[0];
                                String realUserUuid = authData[1]; // Extracts the secure 36-character cloud UUID string!

                                SyncManager syncManager = new SyncManager(LoginActivity.this);
                                syncManager.downloadInventoryFromCloud();

                                Intent intent = new Intent(LoginActivity.this, Dashboard.class);

                                // Pass the secure UUID string for your database sync pipelines
                                intent.putExtra("ACTIVE_USER_UUID", realUserUuid);

                                // Pass the readable text email address string separately for the visual toolbar greeting!
                                intent.putExtra("ACTIVE_USER_EMAIL", username);

                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            }
        });


        //Create account button
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the registration page
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}