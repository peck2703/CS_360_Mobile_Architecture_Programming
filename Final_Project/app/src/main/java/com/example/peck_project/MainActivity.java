package com.example.peck_project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantly check your professional login cache file state
        android.content.SharedPreferences prefs = getSharedPreferences("PeckProjectPrefs", MODE_PRIVATE);
        String savedUserUuid = prefs.getString("LOGGED_IN_USER_UUID", null);

        Intent targetIntent;

        if (savedUserUuid != null && !savedUserUuid.trim().isEmpty()) {
            // Route straight past authentication straight to the inventory desk framework
            targetIntent = new Intent(MainActivity.this, Dashboard.class);
        } else {
            // Route to your functional authentication interface page
            targetIntent = new Intent(MainActivity.this, LoginActivity.class);
        }

        startActivity(targetIntent);
        finish(); // Self-destruct this layout tracking window completely
    }
}
