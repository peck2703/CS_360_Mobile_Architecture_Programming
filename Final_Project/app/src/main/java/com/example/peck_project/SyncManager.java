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
                String jsonResponse = supabase.fetchTableData("Inventory");
                Log.d("SyncManager", "Raw cloud download JSON payload: " + jsonResponse);

                Type inventoryListType = new TypeToken<List<InventoryItem>>(){}.getType();
                List<InventoryItem> cloudItems = gson.fromJson(jsonResponse, inventoryListType);

                if (cloudItems != null) {
                    for (InventoryItem item : cloudItems) {
                        //Ensure your image variable is safe before writing to your app tables
                        if (item.getItemImage() == null) {
                            // If your local model doesn't let you alter values, handle via a safe insertion check
                        }
                        dbHelper.upsertInventoryFromCloud(item);
                    }
                    Log.i("SyncManager", "Cloud download completed successfully. Local lists updated.");
                }
            } catch (Exception e) {
                Log.e("SyncManager", "CRITICAL DOWNLOAD PARSING ERROR AT LINE 39: ");
                e.printStackTrace(); // This prints out the exact text type mismatch in Logcat
            }
        }).start();
    }


    /**
     * Scans local SQLite tables for modified records (is_dirty = 1)
     * and pushes them up to the Supabase PostgreSQL database.
     */
    /**
     * Scans local SQLite tables for modified records (is_dirty = 1)
     * and pushes them up to the Supabase PostgreSQL database.
     */
    public void uploadLocalChangesToCloud() {
        new Thread(() -> {
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;
            try {
                cursor = db.query(
                        "inventory",
                        null,
                        "is_dirty = 1",
                        null, null, null, null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    List<String> syncedIds = new ArrayList<>();

                    do {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                        String userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
                        String sku = cursor.getString(cursor.getColumnIndexOrThrow("sku"));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                        String desc = cursor.getString(cursor.getColumnIndexOrThrow("item_description"));
                        int qty = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));
                        double cost = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
                        double retail = cursor.getDouble(cursor.getColumnIndexOrThrow("retail_price"));
                        int reorder = cursor.getInt(cursor.getColumnIndexOrThrow("reorder_point"));
                        String imgUrl = "";

                        String activeLocationId = "";
                        Cursor locCursor = null;
                        try {
                            locCursor = db.rawQuery("SELECT location_id FROM Inventory_Locations WHERE inventory_id = ?", new String[]{id});
                            if (locCursor != null && locCursor.moveToFirst()) {
                                activeLocationId = locCursor.getString(0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (locCursor != null) locCursor.close();
                        }

                        // If fallback fails to pull a matching value context, assign a placeholder token string
                        if (activeLocationId == null || activeLocationId.trim().isEmpty()) {
                            activeLocationId = "b4b7f207-3ec6-43a5-8af7-bbe5c671e894"; // Uses your current verified active UUID
                        }

                        // Build a single inventory item object model payload string
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
                        obj.addProperty("item_image", imgUrl);

                        // Appends the location identifier parameter natively to clear the schema gate check
                        String activeLocationID = "";
                        try {
                            int locIdx = cursor.getColumnIndex("location_id");
                            if (locIdx != -1 && !cursor.isNull(locIdx)) {
                                activeLocationId = cursor.getString(locIdx);
                            } else {
                                // Fallback: Query the localized layout value out of your tracking link table directly
                                locCursor = db.rawQuery("SELECT location_id FROM Inventory_Locations WHERE inventory_id = ?", new String[]{id});
                                if (locCursor != null) {
                                    if (locCursor.moveToFirst()) {
                                        activeLocationId = locCursor.getString(0);
                                    }
                                    locCursor.close();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (activeLocationId == null || activeLocationId.trim().isEmpty()) {
                            activeLocationId = "b4b7f207-3ec6-43a5-8af7-bbe5c671e894"; // Uses your current verified active UUID code
                        }

                        obj.addProperty("location_id", activeLocationId);

                        // Send this specific single row payload up instantly
                        Log.d("SYNC_DIAGNOSTIC", "Attempting transmission for: " + name + " with JSON: " + obj.toString());
                        boolean uploadSuccess = supabase.upsertData("Inventory", obj.toString());

                        if (uploadSuccess) {
                            Log.i("SyncManager", "Row upload success for ID: " + id);
                            syncedIds.add(id);
                        } else {
                            Log.e("SyncManager", "Row upload explicitly rejected by Supabase API gate.");
                        }
                    } while (cursor.moveToNext());

                    if (!syncedIds.isEmpty()) {
                        dbHelper.clearLocalDirtyFlags("inventory", syncedIds);
                    }
                }

            } catch (Exception e) {
                Log.e("SyncManager", "Network sync upload cycle disconnected drop failure.");
                Log.e("SyncManager", "REAL ERROR FOOTPRINT: ", e);
            } finally {
                if (cursor != null) cursor.close();
            }
        }).start();
    }


}
