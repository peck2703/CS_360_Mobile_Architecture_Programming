package com.example.peck_project;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
        }

        // Capture session contexts sent forward from the Dashboard layout frame
        activeLocationId = getIntent().getStringExtra("LOCATION_ID");
        if (activeLocationId == null) activeLocationId = "default-location-uuid";

        activeUserEmail = getIntent().getStringExtra("ACTIVE_USER");
        if (activeUserEmail == null) activeUserEmail = "default_user";

        spInventoryItems = findViewById(R.id.inventory_items);
        spDiscrepancyTypes = findViewById(R.id.discrepancy_types);
        etReportCount = findViewById(R.id.report_count);
        etReportMessage = findViewById(R.id.report_short_desc);
        btnSubmitDiscrepancy = findViewById(R.id.btn_report_submit);

        dbHelper = new DatabaseHelper(this);
        spinnerDisplayList = new ArrayList<>();
        skuLookupList = new ArrayList<>();

        // Populate spinner with item options assigned strictly to the active location
        populateInventorySpinner();

        btnSubmitDiscrepancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                // DYNAMIC LOOKUP: Fetch item model context directly out of our location database index
                InventoryItemModel itemDetails = dbHelper.getItemBySkuAndLocation(targetSku, activeLocationId);

                if (itemDetails == null) {
                    Toast.makeText(ReportDiscrepancyActivity.this, "Error referencing stock records.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String targetInvId = itemDetails.getId();        // Resolved database Primary Key UUID
                int expectedQuantity = itemDetails.getQuantity(); // Resolved expected baseline software stock quantity

                // Generate layout timeline tags
                String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // CONSTRUCT MODEL: Wrap components into the clean object blueprint matching your helper contract
                AuditModel auditRecord = new AuditModel();
                auditRecord.setAuditsId(java.util.UUID.randomUUID().toString());
                auditRecord.setUserId(activeUserEmail);
                auditRecord.setAuditsInvId(targetInvId);
                auditRecord.setAuditsLocId(activeLocationId);
                auditRecord.setAuditsExpected(expectedQuantity);
                auditRecord.setAuditsActual(reportedActualCount);
                auditRecord.setAuditsStatus(chosenDiscrepancyStatus);
                auditRecord.setAuditsNotes(notesMessage);
                auditRecord.setAuditsCreated(currentTimeStamp);
                auditRecord.setAuditsResolvedAt(""); // Empty field initially until fixed on dashboard

                // EXECUTE: Fire implementation to process metrics and save to SQLite
                boolean isLogged = dbHelper.insertAuditLog(auditRecord);

                if (isLogged) {
                    Toast.makeText(ReportDiscrepancyActivity.this, "Discrepancy logged successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ReportDiscrepancyActivity.this, "Database error logging report.", Toast.LENGTH_SHORT).show();
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

        // ARCHITECTURE RESOLUTION: Pull items assigned to THIS physical facility location
        ArrayList<InventoryItemModel> items = dbHelper.getInventoryByLocation(activeLocationId);

        for (InventoryItemModel item : items) {
            spinnerDisplayList.add(item.getName() + " (#" + item.getSku() + ")");
            skuLookupList.add(item.getSku());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerDisplayList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInventoryItems.setAdapter(spinnerAdapter);
    }
}
