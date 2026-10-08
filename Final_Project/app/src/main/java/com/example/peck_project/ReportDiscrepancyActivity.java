package com.example.peck_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ReportDiscrepancyActivity extends AppCompatActivity {
    private Spinner spInventoryItems, spDiscrepancyTypes;
    private EditText etReportCount, etReportMessage;
    private android.widget.Button btnSubmitDiscrepancy;

    private DatabaseHelper dbHelper;
    private SyncManager syncManager;
    private ArrayList<String> spinnerDisplayList;
    private ArrayList<String> skuLookupList;

    // Track state parameters passed down from session routing context
    private String activeLocationId;
    private String activeUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_audit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back navigation arrow
            getSupportActionBar().setDisplayShowHomeEnabled(true);    // Makes it clickable

            String displayEmail = getIntent().getStringExtra("ACTIVE_USER_EMAIL");
            if (displayEmail == null || displayEmail.trim().isEmpty()) {
                displayEmail = "User";
            }

            TextView tvTitle = findViewById(R.id.toolbar_welcome);
            if (tvTitle != null) {
                String formattedGreeting = String.format(getString(R.string.dashboard_toolbar_welcome), displayEmail);
                tvTitle.setText(formattedGreeting);
            }
        }

        // Capture session contexts sent forward from the Dashboard layout frame
        android.content.SharedPreferences prefs = getSharedPreferences("PeckProjectPrefs", MODE_PRIVATE);
        activeLocationId = prefs.getString("LOGGED_IN_USER_LOCATION_ID", null);
        activeUserEmail = prefs.getString("LOGGED_IN_USER_UUID", null);

        // If the sync data hasn't finished loading yet, notify the user and block entry
        if (activeLocationId == null || activeUserEmail == null) {
            Log.e("SPINNER_DIAGNOSTIC", "Aborting: Profile sync context is missing from SharedPreferences.");
            Toast.makeText(this, "Session data is still loading from cloud. Please try again in a moment.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spInventoryItems = findViewById(R.id.inventory_items);
        spDiscrepancyTypes = findViewById(R.id.discrepancy_types);
        etReportCount = findViewById(R.id.report_count);
        etReportMessage = findViewById(R.id.report_short_desc);
        btnSubmitDiscrepancy = findViewById(R.id.btn_report_submit);

        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
        spinnerDisplayList = new ArrayList<>();
        skuLookupList = new ArrayList<>();

        // Populate spinner with item options assigned strictly to the active location
        populateInventorySpinner();

        btnSubmitDiscrepancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DISCREPANCY_DEBUG", "Step 1: Button Click Triggered");

                android.content.SharedPreferences prefs = ReportDiscrepancyActivity.this.getSharedPreferences("PeckProjectPrefs", MODE_PRIVATE);
                String trueUserUuid = prefs.getString("LOGGED_IN_USER_UUID", null);
                String trueLocationId = prefs.getString("LOGGED_IN_USER_LOCATION_ID", null);

                if (trueUserUuid == null || trueUserUuid.trim().isEmpty() || trueLocationId == null) {
                    Log.e("DISCREPANCY_DEBUG", "Broke at Step 3: Session preferences are empty!");
                    Toast.makeText(ReportDiscrepancyActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();

                    Intent loginIntent = new Intent(ReportDiscrepancyActivity.this, LoginActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                    return;
                }

                if (skuLookupList.isEmpty()) {
                    Toast.makeText(ReportDiscrepancyActivity.this, "No inventory items available to report.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedPosition = spInventoryItems.getSelectedItemPosition();
                String targetSku = skuLookupList.get(selectedPosition);
                String chosenDiscrepancyStatus = spDiscrepancyTypes.getSelectedItem().toString();

                String countRaw = etReportCount.getText().toString().trim();
                String notesMessage = etReportMessage.getText().toString().trim();

                if (countRaw.isEmpty()) {
                    etReportCount.setError("Count is required");
                    return;
                }
                int reportedActualCount = Integer.parseInt(countRaw);

                //Fetch item model context directly out of our location database index
                InventoryItemModel itemDetails = dbHelper.getItemBySkuAndLocation(targetSku, trueLocationId);

                if (itemDetails == null) {
                    Log.e("DISCREPANCY_DEBUG", "Broke at Step 2: itemDetails is NULL");
                    Toast.makeText(ReportDiscrepancyActivity.this, "Error referencing stock records.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String targetInvId = itemDetails.getId();
                if (targetInvId == null || targetInvId.trim().isEmpty()) {
                    Log.e("DISCREPANCY_DEBUG", "Broke: Resolved item has an invalid or null inventory table ID record reference!");
                    Toast.makeText(ReportDiscrepancyActivity.this, "Error processing inventory relationship reference.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (trueUserUuid == null || trueUserUuid.trim().isEmpty()) {
                    // If the cache is empty, read the intent extras passed forward by the login router thread parameters
                    trueUserUuid = getIntent().getStringExtra("ACTIVE_USER_UUID");
                }

                int expectedQuantity = itemDetails.getQuantity(); // Resolved expected baseline stock quantity
                Log.d("DISCREPANCY_DEBUG", "Item resolved successfully: " + itemDetails.getName() + " | Expected Qty: " + expectedQuantity);
                Log.d("DISCREPANCY_DEBUG", "Step 4: Form data validated. User: " + trueUserUuid);

                // Generate timeline tags compliant with PostgreSQL standard format
                String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());

                // CONSTRUCT MODEL: Wrap components into the clean object blueprint matching your helper contract
                AuditModel auditRecord = new AuditModel();
                auditRecord.setAuditsId(java.util.UUID.randomUUID().toString());
                auditRecord.setUserId(trueUserUuid);
                auditRecord.setAuditsInvId(targetInvId);
                auditRecord.setAuditsLocId(trueLocationId);
                auditRecord.setAuditsExpected(expectedQuantity);
                auditRecord.setAuditsActual(reportedActualCount);
                auditRecord.setAuditsStatus(chosenDiscrepancyStatus);
                auditRecord.setAuditsNotes(notesMessage);
                auditRecord.setAuditsCreated(currentTimeStamp);
                auditRecord.setAuditsResolvedAt(""); // Empty field initially until fixed on dashboard

                // EXECUTE: Fire implementation to process metrics and save to SQLite
                boolean isLogged = dbHelper.insertAuditLog(auditRecord);
                Log.d("DISCREPANCY_DEBUG", "Step 5: SQLite Insertion status = " + isLogged);

                if (isLogged) {
                    // Trigger the background remote cloud transmission
                    syncManager.pushAuditToSupabase(auditRecord);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReportDiscrepancyActivity.this, "Discrepancy logged successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReportDiscrepancyActivity.this, "Database error logging report.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SMSActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateInventorySpinner() {
        spinnerDisplayList.clear();
        skuLookupList.clear();

        Log.d("SPINNER_DIAGNOSTIC", "Active Location ID context being passed to SQLite query: [" + activeLocationId + "]");

        // ARCHITECTURE RESOLUTION: Pull items assigned to THIS physical facility location
        ArrayList<InventoryItemModel> items = dbHelper.getInventoryByLocation(activeLocationId);

        // DIAGNOSTIC LOGS: Check how many rows were returned out of the SQLite indices
        Log.d("SPINNER_DIAGNOSTIC", "Total items retrieved from SQLite for this location: " + (items != null ? items.size() : 0));

        if (items != null && !items.isEmpty()) {
            for (InventoryItemModel item : items) {
                spinnerDisplayList.add(item.getName() + " (#" + item.getSku() + ")");
                skuLookupList.add(item.getSku());
            }
        }
        else {
            Log.e("SPINNER_DIAGNOSTIC", "CRITICAL: The item list is empty. No inventory rows found for this location ID.");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerDisplayList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInventoryItems.setAdapter(spinnerAdapter);
    }
}