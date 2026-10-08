package com.example.peck_project;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etEmail, etPassword, etConfirmPassword, etNewLocation;
    Spinner spnLocation;
    CheckBox chCreateNewLoc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        // Map only the required authentication layout elements
        etEmail = findViewById(R.id.register_email_address);
        etPassword = findViewById(R.id.register_password);
        etConfirmPassword = findViewById(R.id.register_re_enter_password);

        //Location specific UI elements
        spnLocation = findViewById(R.id.spinner_location);
        chCreateNewLoc = findViewById(R.id.checkbox_new_location);
        etNewLocation = findViewById(R.id.register_new_location_name);

        android.widget.Button btnSubmitRegistration = findViewById(R.id.btn_register_create);

        chCreateNewLoc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etNewLocation.setVisibility(View.VISIBLE);
                spnLocation.setVisibility(View.GONE); // Optional: hide spinner if creating new
            }
            else {
                etNewLocation.setVisibility(View.GONE);
                spnLocation.setVisibility(View.VISIBLE); // Optional: bring spinner back
            }
        });

        btnSubmitRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                String confirmPass = etConfirmPassword.getText().toString().trim();

                //Final location name based on user selection
                String temporaryStringName = "";
                if (chCreateNewLoc.isChecked()) {
                    temporaryStringName = etNewLocation.getText().toString().trim();
                    if (temporaryStringName.isEmpty()) {
                        etNewLocation.setError("Please enter a valid location name.");
                        return;
                    }
                }
                else {
                    // If the checkbox is NOT checked, pull from the spinner instead
                    if (spnLocation.getSelectedItem() != null) {
                        temporaryStringName = spnLocation.getSelectedItem().toString();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Please select a location.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }


                final String finalLocationName = temporaryStringName;
                // Guard clauses for email/password credentials
                if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!pass.equals(confirmPass)) {
                    etConfirmPassword.setError("Passwords do not match!");
                    return;
                }

                // Move execution off the main UI thread to handle the cloud network request
                new Thread(() -> {
                    try {
                        SupabaseClient client = SupabaseClient.getInstance();
                        String secureCloudUserUuid = client.registerUserAccount(email, pass);

                        runOnUiThread(() -> {
                            if (secureCloudUserUuid != null && !secureCloudUserUuid.trim().isEmpty()) {

                                // Save the actual cloud UUID into the local SQLite users index!
                                dbHelper.registerUser(secureCloudUserUuid, email, pass);

                                // Set up the location model using the matching cloud user token
                                LocationModel userLocation = new LocationModel();
                                userLocation.setId(java.util.UUID.randomUUID().toString()); // Location unique ID can stay random local text
                                userLocation.setUserId(secureCloudUserUuid);              // Bound directly to the true Cloud User UUID!
                                userLocation.setLocationName(finalLocationName);
                                userLocation.setSubLocation("");

                                String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                userLocation.setCreatedAt(currentTimeStamp);

                                // Save the location details locally in SQLite
                                dbHelper.addLocation(userLocation);

                                Toast.makeText(RegisterActivity.this, "Registration Successful! Check email for verification link.", Toast.LENGTH_LONG).show();
                                finish(); // Route user backward cleanly
                            } else {
                                etEmail.setError("This email address is invalid or already registered.");
                                Toast.makeText(RegisterActivity.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (Exception e) {
                        android.util.Log.e("REGISTRATION_GATE", "CRITICAL ACTIVITY SCREEN RUNTIME EXCEPTION:", e);
                        runOnUiThread(() ->
                                Toast.makeText(RegisterActivity.this, "Network timeout. Try again.", Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
            }
        });

        etConfirmPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                String initialPassword = etPassword.getText().toString();
                if(!hasFocus){
                    String confirmPassword = etConfirmPassword.getText().toString();
                    if(!initialPassword.equals(confirmPassword)){
                        etConfirmPassword.setError("Passwords do not match.");
                    } else {
                        etConfirmPassword.setError(null);
                    }
                }
            }
        });

        // Focus listeners to clear inputs dynamically on user click action
        etEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etEmail.setText("");
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
        etConfirmPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etConfirmPassword.setText("");
                }
            }
        });
    }
}
