package com.example.peck_project;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.HashMap;

public class InventoryOrderActivity extends AppCompatActivity {
    private ListView lvOrderItems;
    private android.widget.Button btnSubmitOrder;

    private DatabaseHelper dbHelper;
    private ArrayList<InventoryItem> inventoryItemsList;
    private InventoryOrderAdapter orderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_inventory);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);    // Shows the back arrow graphic
            getSupportActionBar().setDisplayShowHomeEnabled(true);    // Makes it clickable
        }

        lvOrderItems = findViewById(R.id.order_items_view);
        btnSubmitOrder = findViewById(R.id.btn_submit_order);

        dbHelper = new DatabaseHelper(this);
        inventoryItemsList = new ArrayList<>();

        loadDatabaseItems();

        btnSubmitOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, Integer> cartOrders = orderAdapter.getOrderQuantitiesMap();
                boolean transactionSuccess = true;

                for (InventoryItem item : inventoryItemsList) {
                    int quantityOrdered = cartOrders.get(item.getItemNumber());

                    if (quantityOrdered > 0) {
                        int updatedStockBalance = item.getQuantity() + quantityOrdered;
                        boolean rowsUpdated = dbHelper.updateItemQuantity(item.getItemNumber(), updatedStockBalance);
                        if (!rowsUpdated) {
                            transactionSuccess = false;
                        }
                    }
                }

                if (transactionSuccess) {
                    Toast.makeText(InventoryOrderActivity.this, "Bulk order processed successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InventoryOrderActivity.this, "Error writing some order rows.", Toast.LENGTH_SHORT).show();
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


    @Override
    public boolean onSupportNavigateUp() {
        // Closes this activity and returns the user to the calling parent screen (the Dashboard)
        finish();
        return true;
    }

    private void loadDatabaseItems() {
        inventoryItemsList.clear();
        Cursor cursor = dbHelper.getAllItems();

        if (cursor != null) {
            //Guard in case database is empty
            if (cursor.getCount() == 0) {
                cursor.close();
                Toast.makeText(this, "Your inventory catalog is empty. Please add items first!", Toast.LENGTH_LONG).show();
                finish(); // Closes the screen safely and drops the user back to the dashboard
                return;
            }

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                String number = cursor.getString(cursor.getColumnIndexOrThrow("item_number"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("item_description"));
                byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow("item_image"));

                inventoryItemsList.add(new InventoryItem(name, number, qty, desc, img));
            }
            cursor.close();
        }

        orderAdapter = new InventoryOrderAdapter(this, inventoryItemsList);
        lvOrderItems.setAdapter(orderAdapter);
    }
}
