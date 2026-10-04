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

        etUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etUsername.setText("");
                }
            }
        });

        etPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etPassword.setText("");
                }
            }
        });

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
                        String jwtToken = client.authenticateUser(username, password);

                        // Return to UI thread to handle UI visual transitions
                        runOnUiThread(() -> {
                            if (jwtToken != null) {
                                // Trigger background sync immediately now that client has auth clearances
                                SyncManager syncManager = new SyncManager(LoginActivity.this);
                                syncManager.downloadInventoryFromCloud();

                                // Move to app dashboard if successful login
                                Intent intent = new Intent(LoginActivity.this, Dashboard.class);
                                intent.putExtra("ACTIVE_USER", username);
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