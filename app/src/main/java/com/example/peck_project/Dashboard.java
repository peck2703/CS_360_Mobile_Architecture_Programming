package com.example.peck_project;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class Dashboard extends AppCompatActivity {

    // UI Elements for all 4 functional layout parameters
    private TextView tvWelcomeUser, tvLowStockBanner;
    private Button btnViewInventory, btnAddRemoveInventory, btnOrderInventory, btnReportDiscrepancy;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Bind layout schema
        setContentView(R.layout.activity_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);

        //Map layout hooks
        tvWelcomeUser = findViewById(R.id.toolbar_welcome);
        tvLowStockBanner = findViewById(R.id.tv_low_stock_banner);

        if (getIntent().hasExtra("ACTIVE_USER")) {
            String activeUser = getIntent().getStringExtra("ACTIVE_USER");
            tvWelcomeUser.setText("Welcome, " + activeUser + "!");
        }

        //Map all 4 distinct button hooks
        btnViewInventory = findViewById(R.id.btn_view_inventory);
        btnAddRemoveInventory = findViewById(R.id.btn_add_inventory);
        btnOrderInventory = findViewById(R.id.btn_order_inventory);
        btnReportDiscrepancy = findViewById(R.id.btn_report_discrepancy);

        //Button 1 Route: View Inventory
        btnViewInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, InventoryViewActivity.class);
                intent.putExtra("UI_MODE", "VIEWER");
                startActivity(intent);
            }
        });

        //Button 2 Routes: Add/Remove Dialog Selector Switch
        btnAddRemoveInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options = {"Add Item", "Remove Item"};
                new android.app.AlertDialog.Builder(Dashboard.this)
                        .setTitle("Inventory Action")
                        .setItems(options, new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    // Open the add item page
                                    Intent intent = new Intent(Dashboard.this, AddItemActivity.class);
                                    startActivity(intent);
                                } else if (which == 1) {
                                    // Pulls up view inventory grid flagged for item removal
                                    Intent intent = new Intent(Dashboard.this, InventoryViewActivity.class);
                                    intent.putExtra("UI_MODE", "DELETE_MODE");
                                    startActivity(intent);
                                }
                            }
                        })
                        .show();
            }
        });

        //Button 3 Route: Order Inventory
        btnOrderInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, InventoryOrderActivity.class);
                startActivity(intent);
            }
        });

        //Button 4 Route: Report Discrepancy
        btnReportDiscrepancy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, ReportDiscrepancyActivity.class);
                startActivity(intent);
            }
        });
    }

    // 1. This method injects your menu XML file directly into the Toolbar frame
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    // 2. This method listens for clicks on that specific gear icon button
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Direct route: Instantly navigate from Dashboard to SMSActivity
            Intent intent = new Intent(Dashboard.this, SMSActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkInventoryStockLevels();
    }

    private void checkInventoryStockLevels() {
        int lowStockItemsCount = dbHelper.getLowStockCount(5); // Check for items with < 5 units

        if (lowStockItemsCount > 0) {
            // 2. FIXED: Cleaned up the raw URL string escape artifacts
            tvLowStockBanner.setVisibility(View.VISIBLE);
            tvLowStockBanner.setText("WARNING: " + lowStockItemsCount + " Item(s) Low on Stock!");
        } else {
            // Hide it completely if stock across all items is healthy
            tvLowStockBanner.setVisibility(View.GONE);
        }
    }
}
