package com.example.peck_project;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SyncManager {
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final SupabaseClient supabase;
    private final Gson gson;

    public SyncManager(Context context) {
        this.context = context;
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

                com.google.gson.JsonArray array = com.google.gson.JsonParser.parseString(jsonResponse).getAsJsonArray();

                if (array != null && array.size() > 0) {
                    // Dynamically cache the location ID from the very first object payload structure
                    com.google.gson.JsonObject firstObj = array.get(0).getAsJsonObject();
                    if (firstObj.has("location_id") && !firstObj.get("location_id").isJsonNull()) {
                        String dynamicLocationId = firstObj.get("location_id").getAsString();
                        android.content.SharedPreferences prefs = context.getSharedPreferences("PeckProjectPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("LOGGED_IN_USER_LOCATION_ID", dynamicLocationId).apply();
                    }

                    for (int i = 0; i < array.size(); i++) {
                        com.google.gson.JsonObject obj = array.get(i).getAsJsonObject();

                        InventoryItem item = new InventoryItem();
                        item.id = obj.has("id") && !obj.get("id").isJsonNull() ? obj.get("id").getAsString() : "";
                        item.userId = obj.has("user_id") && !obj.get("user_id").isJsonNull() ? obj.get("user_id").getAsString() : "";
                        item.sku = obj.has("sku") && !obj.get("sku").isJsonNull() ? obj.get("sku").getAsString() : "";
                        item.itemName = obj.has("item_name") && !obj.get("item_name").isJsonNull() ? obj.get("item_name").getAsString() : "";
                        item.createdAt = obj.has("created_at") && !obj.get("created_at").isJsonNull() ? obj.get("created_at").getAsString() : "";
                        item.updatedAt = obj.has("updated_at") && !obj.get("updated_at").isJsonNull() ? obj.get("updated_at").getAsString() : "";
                        item.costPrice = obj.has("cost_price") && !obj.get("cost_price").isJsonNull() ? obj.get("cost_price").getAsDouble() : 0.0;
                        item.retailPrice = obj.has("retail_price") && !obj.get("retail_price").isJsonNull() ? obj.get("retail_price").getAsDouble() : 0.0;
                        item.reorderPoint = obj.has("reorder_point") && !obj.get("reorder_point").isJsonNull() ? obj.get("reorder_point").getAsInt() : 0;
                        item.itemImage = obj.has("item_image") && !obj.get("item_image").isJsonNull() ? obj.get("item_image").getAsString() : "";
                        item.locationId = obj.has("location_id") && !obj.get("location_id").isJsonNull() ? obj.get("location_id").getAsString() : "";

                        if (obj.has("item_quantity") && !obj.get("item_quantity").isJsonNull()) {
                            item.itemQuantity = obj.get("item_quantity").getAsInt();
                        } else {
                            item.itemQuantity = 0;
                        }

                        // Write the fully loaded object directly to SQLite
                        dbHelper.upsertInventoryFromCloud(item);
                    }
                    Log.i("SyncManager", "Cloud download completed successfully. Local lists updated with true quantities.");
                }
            } catch (Exception e) {
                Log.e("SyncManager", "CRITICAL DOWNLOAD PARSING ERROR AT LINE 39: ");
                e.printStackTrace();
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

    /**
     * Pushes a single audit discrepancy record up to the Supabase remote database.
     */
    public void pushAuditToSupabase(final AuditModel audit) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //String currentUid = supabase.getCurrentUserUid();

                    // Build a JSON object to match your custom SupabaseClient schema expectations
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    obj.addProperty("id", audit.getAuditsId());
                    obj.addProperty("user_id", audit.getUserId());
                    obj.addProperty("inventory_id", audit.getAuditsInvId());
                    obj.addProperty("location_id", audit.getAuditsLocId());
                    obj.addProperty("expected_qty", audit.getAuditsExpected());
                    obj.addProperty("actual_qty", audit.getAuditsActual());
                    obj.addProperty("status", audit.getAuditsStatus());
                    obj.addProperty("notes", audit.getAuditsNotes());
                    obj.addProperty("created_at", audit.getAuditsCreated());
                    if (audit.getAuditsResolvedAt() != null && !audit.getAuditsResolvedAt().trim().isEmpty()) {
                        obj.addProperty("resolved_at", audit.getAuditsResolvedAt());
                    } else {
                        obj.add("resolved_at", com.google.gson.JsonNull.INSTANCE); // Sends a true database NULL token!
                    }

                    Log.d("SyncManager", "Pushing audit discrepancy to cloud: " + obj.toString());

                    // Execute transmission using your class-level 'supabase' instance and 'upsertData' method
                    boolean uploadSuccess = supabase.upsertData("Audits", obj.toString());

                    if (uploadSuccess) {
                        Log.i("SyncManager", "Success! Audit record committed to Supabase cloud table.");
                    } else {
                        try {
                            Log.e("SUPABASE_SYNC_DEBUG", "API Gateway validation failure checkpoint reached.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Log.e("SyncManager", "Audit upload rejected by Supabase API gateway.");
                    }

                } catch (Exception e) {
                    Log.e("SyncManager", "Error pushing audit record to cloud");
                    e.printStackTrace();
                }
            }
        });
    }
}
