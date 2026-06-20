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

import java.util.ArrayList;

public class ReportDiscrepancyActivity extends AppCompatActivity {
    private Spinner spInventoryItems, spDiscrepancyTypes;
    private EditText etReportCount, etReportMessage;
    private android.widget.Button btnSubmitDiscrepancy;

    private DatabaseHelper dbHelper;
    private ArrayList<String> spinnerDisplayList;
    private ArrayList<String> skuLookupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_audit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        spInventoryItems = findViewById(R.id.inventory_items);
        spDiscrepancyTypes = findViewById(R.id.discrepancy_types);
        etReportCount = findViewById(R.id.report_count);
        etReportMessage = findViewById(R.id.report_short_desc);
        btnSubmitDiscrepancy = findViewById(R.id.btn_report_submit);

        dbHelper = new DatabaseHelper(this);
        spinnerDisplayList = new ArrayList<>();
        skuLookupList = new ArrayList<>();

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
                String chosenDiscrepancyType = spDiscrepancyTypes.getSelectedItem().toString();

                String countRaw = etReportCount.getText().toString().trim();
                String notesMessage = etReportMessage.getText().toString().trim();

                if (countRaw.isEmpty()) {
                    etReportCount.setError("Count is required");
                    return;
                }
                int reportedCount = Integer.parseInt(countRaw);

                boolean isLogged = dbHelper.logDiscrepancy(targetSku, chosenDiscrepancyType, reportedCount, notesMessage);

                if (isLogged) {
                    Toast.makeText(ReportDiscrepancyActivity.this, "Discrepancy logged successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ReportDiscrepancyActivity.this, "Database error logging report.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //Injects the gear icon into this screen's toolbar
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    //Listens for the gear icon click on this screen
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Safe context route: Launch the SMS page from the current activity screen
            Intent intent = new Intent(this, SMSActivity.class);
            startActivity(intent);
            return true;
        }
        //Handle the toolbar back arrow click alongside the gear click
        else if (id == android.R.id.home) {
            finish(); // Closes the current activity screen safely
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateInventorySpinner() {
        spinnerDisplayList.clear();
        skuLookupList.clear();

        Cursor cursor = dbHelper.getAllItems();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                String sku = cursor.getString(cursor.getColumnIndexOrThrow("item_number"));

                spinnerDisplayList.add(name + " (#" + sku + ")");
                skuLookupList.add(sku);
            }
            cursor.close();
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerDisplayList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInventoryItems.setAdapter(spinnerAdapter);
    }
}