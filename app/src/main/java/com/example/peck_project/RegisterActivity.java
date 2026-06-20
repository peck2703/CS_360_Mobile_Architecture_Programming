package com.example.peck_project;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etFirstName, etLastName, etEmail, etPassword, etConfirmPassword;
    private android.widget.Button btnSubmitRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        etFirstName = findViewById(R.id.register_first_name);
        etLastName = findViewById(R.id.register_last_name);
        etEmail = findViewById(R.id.register_email_address);
        etPassword = findViewById(R.id.register_password);
        etConfirmPassword = findViewById(R.id.register_re_enter_password);
        btnSubmitRegistration = findViewById(R.id.btn_register_create);

        btnSubmitRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String first_name = etFirstName.getText().toString().trim();
                String last_name = etLastName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                String confirmPass = etConfirmPassword.getText().toString().trim();

                if (first_name.isEmpty() || last_name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!pass.equals(confirmPass)) {
                    etConfirmPassword.setError("Passwords do not match!");
                    return;
                }

                boolean isSuccess = dbHelper.registerUser(first_name, last_name, email, pass);
                if (isSuccess) {
                    Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    etEmail.setError("This email address is already registered!");
                    Toast.makeText(RegisterActivity.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                }
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

        //Clear fields on click
        etFirstName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etFirstName.setText("");
                }
            }
        });
        etLastName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    etLastName.setText("");
                }
            }
        });
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