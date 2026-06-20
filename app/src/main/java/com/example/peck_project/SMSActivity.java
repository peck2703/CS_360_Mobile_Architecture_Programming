package com.example.peck_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SMSActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;

    // UI Layout Container Elements
    private LinearLayout permissionRequestContainer, smsStatusResponseContainer;
    private TextView smsStatusTitle, smsStatusDesc;
    private Button btnRequestSmsPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_settings);

        // Toolbar Configuration with Back Arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Map Layout UI Component Hooks
        permissionRequestContainer = findViewById(R.id.permission_request_container);
        smsStatusResponseContainer = findViewById(R.id.sms_status_response_container);
        smsStatusTitle = findViewById(R.id.sms_status_title);
        smsStatusDesc = findViewById(R.id.sms_status_desc);
        btnRequestSmsPermissions = findViewById(R.id.btn_request_sms_permissions);

        //Initial State Check: Check if permission is already given
        updateUiBasedOnPermission();

        //Click Listener: Trigger the official Android Permission System dialog box
        btnRequestSmsPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(
                        SMSActivity.this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_CODE
                );
            }
        });
    }

    private void updateUiBasedOnPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permission Granted: Hide the prompt layout card, unhide the Success status card
            permissionRequestContainer.setVisibility(View.GONE);
            smsStatusResponseContainer.setVisibility(View.VISIBLE);

            smsStatusTitle.setText(getString(R.string.sms_status_title_success));
            smsStatusTitle.setTextColor(android.graphics.Color.parseColor("#1B5E20"));
            smsStatusDesc.setText(getString(R.string.sms_status_desc_success));
        } else {
            // Permission Denied/Not Yet Requested: Show the prompt layout card, hide the status card
            permissionRequestContainer.setVisibility(View.VISIBLE);
            smsStatusResponseContainer.setVisibility(View.GONE);
        }
    }

    //Intercept System Callback: Evaluates whether the user clicked "Allow" or "Deny"
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted!", Toast.LENGTH_SHORT).show();
                updateUiBasedOnPermission();
            } else {
                // User clicked Deny: Modify the status layout panel text visually to show failure
                Toast.makeText(this, "SMS Permission Denied.", Toast.LENGTH_SHORT).show();
                permissionRequestContainer.setVisibility(View.GONE);
                smsStatusResponseContainer.setVisibility(View.VISIBLE);

                smsStatusTitle.setText("Permissions Disabled");
                smsStatusTitle.setTextColor(android.graphics.Color.parseColor("#B71C1C")); // Dark Red
                smsStatusDesc.setText("The application requires SMS configurations enabled to process background alert communications.");
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
