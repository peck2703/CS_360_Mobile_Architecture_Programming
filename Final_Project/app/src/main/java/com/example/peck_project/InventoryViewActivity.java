package com.example.peck_project;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;

public class InventoryViewActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;
    ArrayList<InventoryItem> inventoryItemsList;
    private InventoryGridAdapter adapter;
    private boolean isDeleteModeActive = false;
    private ListView lvInventoryItems;
    private ImageButton fabOrderInventory;
    private String activeUiMode = "VIEWER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);    // Shows the back arrow graphic
            getSupportActionBar().setDisplayShowHomeEnabled(true);    // Makes it clickable
        }


        lvInventoryItems = findViewById(R.id.lv_inventory_items);
        dbHelper = new DatabaseHelper(this);
        inventoryItemsList = new ArrayList<>();
        fabOrderInventory = findViewById(R.id.fab_add_order);

        if (getIntent().hasExtra("UI_MODE")) {
            activeUiMode = getIntent().getStringExtra("UI_MODE");
        }

        if ("DELETE_MODE".equals(activeUiMode)) {
            isDeleteModeActive = true;
            fabOrderInventory.setVisibility(View.GONE);
        } else {
            isDeleteModeActive = false;
            fabOrderInventory.setVisibility(View.VISIBLE);
            fabOrderInventory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(InventoryViewActivity.this, InventoryOrderActivity.class);
                    startActivity(intent);
                }
            });
        }

        loadInventoryItems();

        adapter = new InventoryGridAdapter(this, inventoryItemsList, isDeleteModeActive, dbHelper);
        gvInventoryItems.setAdapter(adapter);

        gvInventoryItems.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                InventoryItem clickedItem = inventoryItemsList.get(position);

                if (isDeleteModeActive) {
                    new android.app.AlertDialog.Builder(InventoryViewActivity.this)
                            .setTitle("Delete Item")
                            .setMessage("Are you sure you want to permanently delete " + clickedItem.getName() + "?")
                            .setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    // Drop the row out of SQLite database storage
                                    boolean deleted = dbHelper.deleteItem(clickedItem.getItemNumber());
                                    if (deleted) {
                                        Toast.makeText(InventoryViewActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                                        inventoryItemsList.remove(position); // Remove from our in-memory list
                                        adapter.notifyDataSetChanged();      // Physically update the grid view on screen
                                    } else {
                                        Toast.makeText(InventoryViewActivity.this, "Error deleting item", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        // Closes this activity and returns the user to the calling parent screen (the Dashboard)
        finish();
        return true;
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


    private void loadInventoryItems() {
        inventoryItemsList.clear();
        Cursor cursor = dbHelper.getAllItems();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                String number = cursor.getString(cursor.getColumnIndexOrThrow("item_number"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("item_description"));
                //byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow("item_image"));

                String img = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_INVENTORY_ITEM_IMAGE));
                if (img == null) img = "";
            }
            cursor.close();
        }
    }
}
