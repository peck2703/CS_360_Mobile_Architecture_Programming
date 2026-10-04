package com.example.peck_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etUsername, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply edge-to-edge window insets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize your database helper
        dbHelper = new DatabaseHelper(this);

        // Map the XML input fields using your exact XML layout IDs
        etUsername = findViewById(R.id.app_login_user);
        etPassword = findViewById(R.id.app_login_pass);

        android.widget.Button btnLogin = findViewById(R.id.btn_primary_login);
        android.widget.Button btnCreateAccount = findViewById(R.id.btn_create_login);
        TextView tvForgotPassword = findViewById(R.id.app_forgot_pass_link);

        // Clear default text hints when users click into fields (optional convenience feature)
        etUsername.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) etUsername.setText(""); });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) etPassword.setText(""); });

        // ROUTE: Route user to RegisterActivity when clicking "Create Account"
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // LOGIN FLOW: Authenticate credentials and look up location context
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter your credentials", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Move database/cloud operation off the main UI thread
                new Thread(() -> {
                    try {
                        SupabaseClient client = SupabaseClient.getInstance();
                        // Assume loginUserAccount checks credentials and returns the unique local/cloud UUID String
                        String authenticatedUserUuid = client.authenticateUser(email, password);

                        runOnUiThread(() -> {
                            if (authenticatedUserUuid != null && !authenticatedUserUuid.isEmpty()) {
                                // Lookup location profile via the database helper using the model query
                                LocationModel userLocation = dbHelper.getLocationByUserId(authenticatedUserUuid);

                                if (userLocation != null) {
                                    // Location found! Move forward to your Inventory Dashboard Activity
                                    Intent intent = new Intent(MainActivity.this, Dashboard.class);
                                    intent.putExtra("LOCATION_NAME", userLocation.getLocationName());
                                    startActivity(intent);
                                    finish(); // Close login window context
                                } else {
                                    // Edge case: User authenticated, but somehow has no location assigned
                                    Toast.makeText(MainActivity.this, "Please configure your location settings.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                                    startActivity(intent);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            }
        });
    }
}
