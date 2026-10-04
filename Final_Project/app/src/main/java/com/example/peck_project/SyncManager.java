package com.example.peck_project;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class SyncManager {
    private final DatabaseHelper dbHelper;
    private final SupabaseClient supabase;
    private final Gson gson;

    public SyncManager(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.supabase = SupabaseClient.getInstance();
        this.gson = new Gson();
    }

    /**
     * Downloads recent updates from Supabase and overwrites the local SQLite cache
     */
    public void downloadInventoryFromCloud() {
        new Thread(() -> {
            try {
                // 1. Fetch data string from your capitalized table name
                String jsonResponse = supabase.fetchTableData("Inventory");

                // 2. Map payload dynamically into your InventoryItem data structures
                Type inventoryListType = new TypeToken<List<InventoryItem>>(){}.getType();
                List<InventoryItem> cloudItems = gson.fromJson(jsonResponse, inventoryListType);

                // 3. Populate your SQLite database container safely
                if (cloudItems != null) {
                    for (InventoryItem item : cloudItems) {
                        dbHelper.upsertInventoryFromCloud(item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Catch connectivity issues gracefully
            }
        }).start();
    }

    /**
     * Scans local SQLite tables for modified records (is_dirty = 1)
     * and pushes them up to the Supabase PostgreSQL database.
     */
    public void uploadLocalChangesToCloud() {
        new Thread(() -> {
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                // 1. Query the local SQLite database for un-synced inventory rows
                // Enforce "is_dirty = 1" criteria loop check
                cursor = db.query(
                        "inventory", // TABLE_INVENTORY
                        null,
                        "is_dirty = 1",
                        null, null, null, null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
                    List<String> syncedIds = new ArrayList<>();

                    do {
                        // Extract local parameters cleanly from cursor positions
                        String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                        String userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
                        String sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                        String desc = cursor.getString(cursor.getColumnIndexOrThrow("item_description"));
                        int qty = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));
                        double cost = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
                        double retail = cursor.getDouble(cursor.getColumnIndexOrThrow("retail_price"));
                        int reorder = cursor.getInt(cursor.getColumnIndexOrThrow("reorder_point"));
                        String img = cursor.getString(cursor.getColumnIndexOrThrow("item_image"));

                        // Construct a matching JSON object payload
                        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                        obj.addProperty("id", id);
                        obj.addProperty("user_id", userId);
                        obj.addProperty("sku", sku);
                        obj.addProperty("item_name", name);
                        obj.addProperty("item_description", desc);
                        obj.addProperty("item_quantity", qty);
                        obj.addProperty("cost_price", cost);
                        obj.addProperty("retail_price", retail);
                        obj.addProperty("reorder_point", reorder);
                        obj.addProperty("item_image", img);

                        jsonArray.add(obj);
                        syncedIds.add(id); // Track ID to clear dirty status later
                    } while (cursor.moveToNext());

                    // 2. Transmit packet string up to your capitalized Supabase table route
                    if (jsonArray.size() > 0) {
                        boolean uploadSuccess = supabase.upsertData("Inventory", jsonArray.toString());

                        if (uploadSuccess) {
                            Log.i("SyncManager", "Batch upload success. Clearing local dirty states.");
                            // 3. Clear dirty status flags inside SQLite layer to complete transaction loop
                            dbHelper.clearLocalDirtyFlags("inventory", syncedIds);
                        } else {
                            Log.e("SyncManager", "Supabase batch upsert rejected payload.");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SyncManager", "Network sync upload cycle disconnected drop failure.");
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }
        }).start();
    }
}
