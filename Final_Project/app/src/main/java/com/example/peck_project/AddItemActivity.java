package com.example.peck_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class AddItemActivity extends AppCompatActivity {
    private EditText etAddName, etAddNumber, etAddQuantity, etAddCost,
            etAddRetail, etAddReorder, etAddDescription;
    private android.widget.Button btnSubmitNewItem;
    private ImageButton btnScanSku;
    private DatabaseHelper dbHelper;
    private String activeLocationName;

    // Register the Result Launcher to handle data coming back from the camera scanner activity
    private ActivityResultLauncher<Intent> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("LOCATION_NAME")) {
            activeLocationName = getIntent().getStringExtra("LOCATION_NAME");
        } else {
            activeLocationName = "Default Location"; // Safe fallback
        }

        etAddName = findViewById(R.id.et_add_name);
        etAddNumber = findViewById(R.id.et_add_number);
        etAddQuantity = findViewById(R.id.et_add_quantity);
        etAddCost = findViewById(R.id.et_add_cost);
        etAddRetail = findViewById(R.id.et_add_retail);
        etAddReorder = findViewById(R.id.et_add_reorder);
        etAddDescription = findViewById(R.id.et_add_desc);
        btnSubmitNewItem = findViewById(R.id.btn_add_item_submit_item);
        btnScanSku = findViewById(R.id.btn_scan_sku);

        //Initialize the launcher callback to capture the scanned barcode string
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedCode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (scannedCode != null) {
                            etAddNumber.setText(scannedCode); // Auto-fill the SKU text field box instantly!
                            Toast.makeText(this, "Barcode captured!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        //Set the camera button action to open the scanner
        btnScanSku.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddItemActivity.this, BarcodeScannerActivity.class);
                scannerLauncher.launch(intent);
            }
        });

        btnSubmitNewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Extract and trim raw string values
                String itemName = etAddName.getText().toString().trim();
                String itemNumber = etAddNumber.getText().toString().trim();
                String itemDesc = etAddDescription.getText().toString().trim();
                String rawQty = etAddQuantity.getText().toString().trim();
                String rawCost = etAddCost.getText().toString().trim();
                String rawRetail = etAddRetail.getText().toString().trim();
                String rawReorder = etAddReorder.getText().toString().trim();

                if (itemName.isEmpty()) {
                    etAddName.setError("Item name is required");
                    return;
                }
                if (itemNumber.isEmpty()) {
                    etAddNumber.setError("Item number is required");
                    return;
                }
                if (itemDesc.isEmpty()) {
                    etAddDescription.setError("Description is required");
                    return;
                }

                int quantity = rawQty.isEmpty() ? 0 : Integer.parseInt(rawQty);
                double cost = rawCost.isEmpty() ? 0.0 : Double.parseDouble(rawCost);
                double retail = rawRetail.isEmpty() ? 0.0 : Double.parseDouble(rawRetail);
                int reorderPoint = rawReorder.isEmpty() ? 0 : Integer.parseInt(rawReorder);

                //Generate unique key identities matching your schema definitions
                String generatedItemId = java.util.UUID.randomUUID().toString();

                //Fetch the active user's name or tracking email passed into this activity intent
                String activeUserEmail = getIntent().getStringExtra("ACTIVE_USER");
                if (activeUserEmail == null) activeUserEmail = "default_user";

                byte[] emptyImageBlob = null;

                //Execute the call with all 10 properties matching your signature perfectly
                boolean isInserted = dbHelper.addItem(
                        generatedItemId,     // 1. id
                        activeUserEmail,     // 2. user_id
                        itemNumber,          // 3. sku
                        itemName,            // 4. item_name
                        itemDesc,            // 5. item_description
                        quantity,            // 6. item_quantity (Uses user input value)
                        cost,                // 7. cost_price (Uses user input value)
                        retail,              // 8. retail_price (Uses user input value)
                        reorderPoint,        // 9. reorder_point (Uses user input value)
                        emptyImageBlob,      // 10. item_image (BLOB payload metadata)
                        activeLocationName     // 11. location_id (Foreign Key mapping link string)
                );

                if (isInserted) {
                    //Upload the changes to the cloud
                    SyncManager syncManager = new SyncManager(AddItemActivity.this);
                    syncManager.uploadLocalChangesToCloud();

                    Toast.makeText(AddItemActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    etAddNumber.setError("This item number already exists!");
                    Toast.makeText(AddItemActivity.this, "Failed to add item.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}