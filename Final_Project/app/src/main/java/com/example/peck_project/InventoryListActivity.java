package com.example.peck_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import com.example.peck_project.InventoryItemModel;

public class InventoryListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvInventoryItems; // Changed to ListView
    private ArrayList<InventoryItemModel> inventoryList;
    private ArrayAdapter<InventoryItemModel> listAdapter;

    private String activeLocationId;
    private String uiMode;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_list);

        dbHelper = new DatabaseHelper(this);
        lvInventoryItems = findViewById(R.id.lv_inventory_items);
        ImageButton fabAddOrder = findViewById(R.id.fab_add_order);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        activeLocationId = getIntent().getStringExtra("LOCATION_ID");
        if (activeLocationId == null) activeLocationId = "";

        uiMode = getIntent().getStringExtra("UI_MODE");
        if (uiMode == null) uiMode = "VIEWER";

        if ("DELETE_MODE".equals(uiMode)) {
            fabAddOrder.setVisibility(View.GONE);
        } else {
            fabAddOrder.setVisibility(View.VISIBLE);
            fabAddOrder.setOnClickListener(v -> {
                Intent intent = new Intent(InventoryListActivity.this, AddItemActivity.class);
                intent.putExtra("LOCATION_ID", activeLocationId);
                startActivity(intent);
            });
        }

        lvInventoryItems.setOnItemClickListener((parent, view, position, id) -> {
            InventoryItemModel selectedItem = inventoryList.get(position);

            if ("DELETE_MODE".equals(uiMode)) {
                new AlertDialog.Builder(InventoryListActivity.this)
                        .setTitle("Delete Item Globally") // Clear title change
                        .setMessage("Are you sure you want to completely delete " + selectedItem.getName() + " from the entire app catalog?")
                        .setPositiveButton("Delete", (dialog, which) -> {

                            // FIXED: Changed to call your global item delete method name
                            boolean isDeleted = dbHelper.deleteItemGlobally(selectedItem.getId());

                            if (isDeleted) {
                                Toast.makeText(InventoryListActivity.this, "Item deleted globally.", Toast.LENGTH_SHORT).show();
                                refreshListData(); // Refresh the card list view rows
                            } else {
                                Toast.makeText(InventoryListActivity.this, "Failed to delete item.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                Toast.makeText(InventoryListActivity.this, "Item: " + selectedItem.getName() + "\nStock: " + selectedItem.getQuantity(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshListData();
    }

    private void refreshListData() {
        inventoryList = dbHelper.getInventoryByLocation(activeLocationId);

        // Map layout fields to our horizontal card elements
        listAdapter = new ArrayAdapter<InventoryItemModel>(this, R.layout.item_inventory_list_card, inventoryList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_inventory_list_card, parent, false);
                }

                InventoryItemModel item = getItem(position);

                TextView tvName = convertView.findViewById(R.id.tv_card_item_name);
                TextView tvSku = convertView.findViewById(R.id.tv_card_item_sku);
                TextView tvQty = convertView.findViewById(R.id.tv_card_item_qty);

                if (item != null) {
                    tvName.setText(item.getName());
                    tvSku.setText("SKU: " + item.getSku());
                    tvQty.setText(String.valueOf(item.getQuantity()));
                }

                return convertView;
            }
        };

        lvInventoryItems.setAdapter(listAdapter);
    }
}
