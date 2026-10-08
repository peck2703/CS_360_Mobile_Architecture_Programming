package com.example.peck_project;
import com.example.peck_project.InventoryItemModel;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Logins.db";
    private static final int DATABASE_VERSION = 4; // Version 4 handles users, inventory, and audits using Supabase

    // Table: Users
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_EMAIL = "email_address";
    private static final String COL_USER_PASSWORD_HASH = "password_hash";
    private static final String COL_USER_CREATED_AT = "created_at";
    private static final String COL_USER_UPDATED_AT = "updated_at";

    // Table: Inventory
    private static final String TABLE_INVENTORY = "inventory";
    private static final String COL_INVENTORY_ITEM_ID = "id";
    private static final String COL_INVENTORY_USER_ID = "user_id";
    private static final String COL_INVENTORY_SKU = "sku";
    private static final String COL_INVENTORY_ITEM_NAME = "item_name";
    private static final String COL_INVENTORY_ITEM_DESC = "item_description";
    private static final String COL_INVENTORY_ITEM_QTY = "item_quantity";
    private static final String COL_INVENTORY_CREATED_AT = "created_at";
    private static final String COL_INVENTORY_UPDATED_AT = "updated_at";
    private static final String COL_INVENTORY_COST_PRICE = "cost_price";
    private static final String COL_INVENTORY_RETAIL_PRICE = "retail_price";
    private static final String COL_INVENTORY_REORDER_POINT = "reorder_point";
    public static final String COL_INVENTORY_ITEM_IMAGE = "item_image";
    private static final String COL_INVENTORY_LOCATION_ID = "location_id";

    //Table: Locations
    private static final String TABLE_LOCATIONS = "locations";
    private static final String COL_LOCATIONS_ID = "id";
    private static final String COL_LOCATIONS_USER = "user_id";
    private static final String COL_LOCATIONS_NAME = "location_name";
    private static final String COL_LOCATIONS_SUB_LOCATION = "sub_location";
    private static final String COL_LOCATIONS_CREATED_AT = "created_at";

    //Table Inventory_Locations
    private static final String TABLE_INVENTORY_LOCATIONS = "inventory_locations";
    private static final String COL_INV_LOC_INV_ID = "inv_id";
    private static final String COL_INV_LOC_LOC_ID = "loc_id";
    private static final String COL_INV_LOC_QUANTITY = "quantity";

    //Table: Barcodes
    private static final String TABLE_BARCODES = "barcodes";
    private static final String COL_BARCODES_ID = "barcode_id";
    private static final String COL_BARCODES_INV_ID = "inv_id";
    private static final String COL_BARCODES_VALUE = "barcode_value";
    private static final String COL_BARCODES_TYPE = "barcode_type";

    //Table: Stock Movements
    private static final String TABLE_STOCK_MOVEMENTS = "stock_movements";
    private static final String COL_STOCK_MOVEMENTS_ID = "stock_movement_id";
    private static final String COL_STOCK_MOVEMENTS_USER_ID = "stock_movement_user_id";
    private static final String COL_STOCK_MOVEMENTS_INV_ID = "stock_movement_inv_id";
    private static final String COL_STOCK_MOVEMENTS_LOCATION_ID = "stock_movement_loc_id";
    private static final String COL_STOCK_MOVEMENTS_QUANTITY_CHANGE = "stock_movement_quan_change";
    private static final String COL_STOCK_MOVEMENTS_REASON = "stock_movement_reason";
    private static final String COL_STOCK_MOVEMENTS_CREATED_AT = "stock_movement_created_at";

    //Table: Audits
    private static final String TABLE_AUDITS = "audits";
    private static final String COL_AUDITS_ID = "audits_id";
    private static final String COL_AUDITS_USER_ID = "user_id";
    private static final String COL_AUDITS_INV_ID = "audits_inv_id";
    private static final String COL_AUDITS_LOC_ID = "audits_loc_id";
    private static final String COL_AUDITS_EXP_QTY = "audits_expected";
    private static final String COL_AUDITS_ACT_QTY = "audits_actual";
    private static final String COL_AUDITS_STATUS = "audits_status";
    private static final String COL_AUDITS_NOTES = "audits_notes";
    private static final String COL_AUDITS_CREATED_AT = "audits_created";
    private static final String COL_AUDITS_RESOLVED_AT = "audits_resolved_at";

    // Cache Management Meta-Columns
    private static final String COL_IS_DIRTY = "is_dirty";         // 1 if updated offline, 0 if synced
    private static final String COL_LAST_UPDATED = "last_updated";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Create Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " TEXT PRIMARY KEY, " + // Supabase auth UUIDs are strings
                COL_USER_EMAIL + " TEXT UNIQUE, " +
                COL_USER_PASSWORD_HASH + " TEXT, " +
                COL_USER_CREATED_AT + " TEXT, " +
                COL_USER_UPDATED_AT + " TEXT)";
        db.execSQL(createUsersTable);

        //Create Locations Table
        String createLocationsTable = "CREATE TABLE " + TABLE_LOCATIONS + " (" +
                COL_LOCATIONS_ID + " TEXT PRIMARY KEY, " + // UUID from Supabase
                COL_LOCATIONS_USER + " TEXT, " +
                COL_LOCATIONS_NAME + " TEXT, " +
                COL_LOCATIONS_SUB_LOCATION + " TEXT, " +
                COL_LOCATIONS_CREATED_AT + " TEXT, " +
                "is_dirty INTEGER DEFAULT 0)"; // Cache synchronization flag
        db.execSQL(createLocationsTable);

        //Create Inventory Table
        String createInventoryTable = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COL_INVENTORY_ITEM_ID + " TEXT PRIMARY KEY, " + // UUID from Supabase
                COL_INVENTORY_USER_ID + " TEXT, " +
                COL_INVENTORY_SKU + " TEXT UNIQUE, " +
                COL_INVENTORY_ITEM_NAME + " TEXT, " +
                COL_INVENTORY_ITEM_DESC + " TEXT, " +
                COL_INVENTORY_ITEM_QTY + " INTEGER, " + // int8 maps cleanly to SQLite INTEGER
                COL_INVENTORY_COST_PRICE + " REAL, " +    // Numeric/decimal fields use REAL
                COL_INVENTORY_RETAIL_PRICE + " REAL, " +  // Numeric/decimal fields use REAL
                COL_INVENTORY_REORDER_POINT + " INTEGER, " +
                COL_INVENTORY_ITEM_IMAGE + " BLOB, " +
                COL_INVENTORY_CREATED_AT + " TEXT, " +
                COL_INVENTORY_UPDATED_AT + " TEXT, " +
                "is_dirty INTEGER DEFAULT 0)"; // Cache synchronization flag
        db.execSQL(createInventoryTable);



        //Create Inventory_Locations Table (Your Composite Primary Key Table)
        String createInventoryLocationsTable = "CREATE TABLE " + TABLE_INVENTORY_LOCATIONS + " (" +
                COL_INV_LOC_INV_ID + " TEXT NOT NULL, " +
                COL_INV_LOC_LOC_ID + " TEXT NOT NULL, " +
                COL_INV_LOC_QUANTITY + " INTEGER NOT NULL, " +
                "is_dirty INTEGER DEFAULT 0, " + // Cache synchronization flag
                "PRIMARY KEY (" + COL_INV_LOC_INV_ID + ", " + COL_INV_LOC_LOC_ID + "))";
        db.execSQL(createInventoryLocationsTable);

        // Create Barcodes Table (Fixed target mapping name string assignment)
        String createBarcodesTable = "CREATE TABLE barcodes (" +
                COL_BARCODES_ID + " TEXT PRIMARY KEY, " + // UUID from Supabase
                COL_BARCODES_INV_ID + " TEXT, " +
                COL_BARCODES_VALUE + " TEXT UNIQUE, " +
                COL_BARCODES_TYPE + " TEXT, " +
                "is_dirty INTEGER DEFAULT 0)"; // Cache synchronization flag
        db.execSQL(createBarcodesTable);

        //Create Stock Movements Table
        String createStockMovementsTable = "CREATE TABLE " + TABLE_STOCK_MOVEMENTS + " (" +
                COL_STOCK_MOVEMENTS_ID + " TEXT PRIMARY KEY, " + // UUID from Supabase
                COL_STOCK_MOVEMENTS_USER_ID + " TEXT, " +
                COL_STOCK_MOVEMENTS_INV_ID + " TEXT, " +
                COL_STOCK_MOVEMENTS_LOCATION_ID + " TEXT, " +
                COL_STOCK_MOVEMENTS_QUANTITY_CHANGE + " INTEGER, " +
                COL_STOCK_MOVEMENTS_REASON + " TEXT, " +
                COL_STOCK_MOVEMENTS_CREATED_AT + " TEXT, " +
                "is_dirty INTEGER DEFAULT 0)"; // Cache synchronization flag
        db.execSQL(createStockMovementsTable);

        //Create Audits Table
        String createAuditsTable = "CREATE TABLE " + TABLE_AUDITS + " (" +
                COL_AUDITS_ID + " TEXT PRIMARY KEY, " + // UUID from Supabase
                COL_AUDITS_USER_ID + " TEXT, " +
                COL_AUDITS_INV_ID + " TEXT, " +
                COL_AUDITS_LOC_ID + " TEXT, " +
                COL_AUDITS_EXP_QTY + " INTEGER, " +
                COL_AUDITS_ACT_QTY + " INTEGER, " +
                COL_AUDITS_STATUS + " TEXT, " +
                COL_AUDITS_NOTES + " TEXT, " +
                COL_AUDITS_CREATED_AT + " TEXT, " +
                COL_AUDITS_RESOLVED_AT + " TEXT, " +
                "is_dirty INTEGER DEFAULT 0)"; // Cache synchronization flag
        db.execSQL(createAuditsTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BARCODES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STOCK_MOVEMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDITS);
        onCreate(db);
    }

    //Grabs any locally modified inventory rows that need to be pushed up to Supabase.
    public Cursor getUnsyncedInventory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_INVENTORY, null, "is_dirty = 1", null, null, null, null);
    }

    //Clears the dirty flag once Retrofit successfully updates the cloud.
    public void markInventoryAsSynced(String itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_dirty", 0);
        db.update(TABLE_INVENTORY, values, COL_INVENTORY_ITEM_ID + " = ?", new String[]{itemId});
    }

    // USER OPERATIONS
    public boolean registerUser(String uuid, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Map the properties directly to your constant column keys
        values.put(COL_USER_ID, uuid); // Saves the auto-generated local UUID string
        values.put(COL_USER_EMAIL, email);
        values.put(COL_USER_PASSWORD_HASH, password);
        values.put(COL_IS_DIRTY, 0); // Mark clean since it was generated through the cloud network logic step above
        values.put(COL_LAST_UPDATED, String.valueOf(System.currentTimeMillis()));

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUserCredentials(String email, String passwordHash) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_USER_EMAIL + " = ? AND " + COL_USER_PASSWORD_HASH + " = ?";
        String[] selectionArgs = {email, passwordHash};
        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    //LOCATION OPERATIONS
    public boolean addLocation(LocationModel location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_LOCATIONS_ID, location.getId()); // Supabase UUID string
        values.put(COL_LOCATIONS_USER, location.getUserId());
        values.put(COL_LOCATIONS_NAME, location.getLocationName());
        values.put(COL_LOCATIONS_SUB_LOCATION, location.getSubLocation());
        values.put(COL_LOCATIONS_CREATED_AT, location.getCreatedAt());
        values.put("is_dirty", 0); // Default synchronization cache status flag

        long result = db.insert(TABLE_LOCATIONS, null, values);
        db.close();
        return result != -1;
    }


    //Get locations by User ID
    public LocationModel getLocationByUserId(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        LocationModel location = null;

        String[] columns = {
                COL_LOCATIONS_ID,
                COL_LOCATIONS_USER,
                COL_LOCATIONS_NAME,
                COL_LOCATIONS_SUB_LOCATION,
                COL_LOCATIONS_CREATED_AT
        };

        Cursor cursor = db.query(
                TABLE_LOCATIONS,
                columns,
                COL_LOCATIONS_USER + " = ?",
                new String[]{userId},
                null, null, null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                location = new LocationModel();
                location.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATIONS_ID)));
                location.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATIONS_USER)));
                location.setLocationName(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATIONS_NAME)));
                location.setSubLocation(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATIONS_SUB_LOCATION)));
                location.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATIONS_CREATED_AT)));
            }
            cursor.close();
        }
        db.close();
        return location;
    }


    // INVENTORY OPERATIONS

    public ArrayList<InventoryItemModel> getInventoryByLocation(String locationId) {
        ArrayList<InventoryItemModel> itemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // FIXED: Changed il.COL_INV_LOC_QUANTITY to i.COL_INVENTORY_ITEM_QTY
        String query = "SELECT i." + COL_INVENTORY_ITEM_ID + ", i." + COL_INVENTORY_ITEM_NAME + ", i." + COL_INVENTORY_SKU + ", i." + COL_INVENTORY_ITEM_QTY
                + " FROM " + TABLE_INVENTORY + " i "
                + " INNER JOIN " + TABLE_INVENTORY_LOCATIONS + " il "
                + " ON i." + COL_INVENTORY_ITEM_ID + " = il." + COL_INV_LOC_INV_ID
                + " WHERE il." + COL_INV_LOC_LOC_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{locationId});

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_ID);
            int nameIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_NAME);
            int skuIdx = cursor.getColumnIndex(COL_INVENTORY_SKU);
            int qtyIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_QTY); // Read out of core item table index

            while (cursor.moveToNext()) {
                String resolvedId = (idIdx != -1) ? cursor.getString(idIdx) : "";
                String resolvedName = (nameIdx != -1) ? cursor.getString(nameIdx) : "Unknown";
                String resolvedSku = (skuIdx != -1) ? cursor.getString(skuIdx) : "";
                int resolvedQty = (qtyIdx != -1) ? cursor.getInt(qtyIdx) : 0;

                InventoryItemModel item = new InventoryItemModel(resolvedId, resolvedName, resolvedSku, resolvedQty);
                item.setLocationId(locationId);
                itemList.add(item);
            }
            cursor.close();
        }
        db.close();
        return itemList;
    }

    public boolean addItem(String itemId, String userId, String sku, String name,
                           String description, int quantity, double cost, double retail,
                           int reorderPoint, byte[] imageBytes, String locationId) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction(); // Wrap in transaction to avoid orphan items if one statement fails

        try {
            // Write product to global Inventory list catalog
            ContentValues itemValues = new ContentValues();
            itemValues.put(COL_INVENTORY_ITEM_ID, itemId);
            itemValues.put(COL_INVENTORY_USER_ID, userId);
            itemValues.put(COL_INVENTORY_SKU, sku);
            itemValues.put(COL_INVENTORY_ITEM_NAME, name);
            itemValues.put(COL_INVENTORY_ITEM_DESC, description);
            itemValues.put(COL_INVENTORY_ITEM_QTY, quantity);
            itemValues.put(COL_INVENTORY_COST_PRICE, cost);
            itemValues.put(COL_INVENTORY_RETAIL_PRICE, retail);
            itemValues.put(COL_INVENTORY_REORDER_POINT, reorderPoint);
            itemValues.put(COL_INVENTORY_ITEM_IMAGE, imageBytes);
            itemValues.put("is_dirty", 1);

            long itemResult = db.insert(TABLE_INVENTORY, null, itemValues);

            // Map item directly to specific physical Location link table
            ContentValues locValues = new ContentValues();
            locValues.put(COL_INV_LOC_INV_ID, itemId);
            locValues.put(COL_INV_LOC_LOC_ID, locationId);
            locValues.put(COL_INV_LOC_QUANTITY, quantity);
            locValues.put("is_dirty", 1);

            long locResult = db.insert(TABLE_INVENTORY_LOCATIONS, null, locValues);

            if (itemResult != -1 && locResult != -1) {
                db.setTransactionSuccessful(); // Only saves changes permanently if both tables confirm write
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error cascading inventory table transaction operations", e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_INVENTORY, null, null, null, null, null, null);
    }
    public boolean updateItemQuantity(String itemId, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_INVENTORY_ITEM_QTY, newQuantity);
        contentValues.put("is_dirty", 1); // Local change requires a cloud sync

        int rowsAffected = db.update(TABLE_INVENTORY, contentValues, COL_INVENTORY_ITEM_ID + " = ?", new String[]{itemId});
        return rowsAffected > 0;
    }

    public boolean deleteItemGlobally(String itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction(); // Start transaction to guarantee both deletions succeed together

        try {
            //First, wipe out all location links for this item to satisfy constraints
            db.delete(TABLE_INVENTORY_LOCATIONS, COL_INV_LOC_INV_ID + " = ?", new String[]{itemId});

            //Next, delete the core item definition out of the master inventory table
            int rowsDeleted = db.delete(TABLE_INVENTORY, COL_INVENTORY_ITEM_ID + " = ?", new String[]{itemId});

            if (rowsDeleted > 0) {
                db.setTransactionSuccessful(); // Finalize save
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error performing cascade delete on item " + itemId, e);
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // AUDIT OPERATIONS
    // Inside DatabaseHelper.java:

    public boolean insertAuditLog(AuditModel audit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        // Map all properties to your exact table column constants
        cv.put(COL_AUDITS_ID, audit.getAuditsId());
        cv.put(COL_AUDITS_USER_ID, audit.getUserId());
        cv.put(COL_AUDITS_INV_ID, audit.getAuditsInvId());
        cv.put(COL_AUDITS_LOC_ID, audit.getAuditsLocId());
        cv.put(COL_AUDITS_EXP_QTY, audit.getAuditsExpected());
        cv.put(COL_AUDITS_ACT_QTY, audit.getAuditsActual());
        cv.put(COL_AUDITS_STATUS, audit.getAuditsStatus());
        cv.put(COL_AUDITS_NOTES, audit.getAuditsNotes());
        cv.put(COL_AUDITS_CREATED_AT, audit.getAuditsCreated());
        cv.put(COL_AUDITS_RESOLVED_AT, audit.getAuditsResolvedAt());

        // CACHE SYNC FLAG: Set to 1 so the app knows this record needs to sync to Supabase
        cv.put("is_dirty", 1);

        // Insert into your defined table constant
        long result = db.insert(TABLE_AUDITS, null, cv);
        db.close();

        return result != -1; // Returns true if insertion succeeds
    }



    public InventoryItemModel getItemBySkuAndLocation(String sku, String locationId) {
        InventoryItemModel item = null;
        SQLiteDatabase db = this.getReadableDatabase();

        // Changed il.COL_INV_LOC_QUANTITY to i.COL_INVENTORY_ITEM_QTY
        String query = "SELECT i." + COL_INVENTORY_ITEM_ID + ", i." + COL_INVENTORY_ITEM_NAME + ", i." + COL_INVENTORY_SKU + ", i." + COL_INVENTORY_ITEM_QTY
                + " FROM " + TABLE_INVENTORY + " i "
                + " INNER JOIN " + TABLE_INVENTORY_LOCATIONS + " il "
                + " ON i." + COL_INVENTORY_ITEM_ID + " = il." + COL_INV_LOC_INV_ID
                + " WHERE i." + COL_INVENTORY_SKU + " = ? AND il." + COL_INV_LOC_LOC_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{sku, locationId});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_ID);
                int nameIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_NAME);
                int skuIdx = cursor.getColumnIndex(COL_INVENTORY_SKU);
                int qtyIdx = cursor.getColumnIndex(COL_INVENTORY_ITEM_QTY);

                if (idIdx != -1 && nameIdx != -1 && skuIdx != -1 && qtyIdx != -1) {
                    item = new InventoryItemModel(
                            cursor.getString(idIdx),
                            cursor.getString(nameIdx),
                            cursor.getString(skuIdx),
                            cursor.getInt(qtyIdx)
                    );
                    item.setLocationId(locationId);
                }
            }
            cursor.close();
        }
        db.close();
        return item;
    }


    public void upsertInventoryFromCloud(InventoryItem item) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Core Inventory Table Update Payload
        ContentValues inventoryValues = new ContentValues();
        inventoryValues.put(COL_INVENTORY_ITEM_ID, item.id);
        inventoryValues.put(COL_INVENTORY_USER_ID, item.userId);
        inventoryValues.put(COL_INVENTORY_ITEM_NAME, item.getName());
        inventoryValues.put(COL_INVENTORY_SKU, item.getItemNumber());
        inventoryValues.put(COL_INVENTORY_CREATED_AT, item.createdAt);
        inventoryValues.put(COL_INVENTORY_UPDATED_AT, item.updatedAt);
        inventoryValues.put(COL_INVENTORY_COST_PRICE, item.costPrice);
        inventoryValues.put(COL_INVENTORY_RETAIL_PRICE, item.retailPrice);
        inventoryValues.put(COL_INVENTORY_REORDER_POINT, item.reorderPoint);
        inventoryValues.put(COL_INVENTORY_ITEM_IMAGE, item.getItemImage());
        inventoryValues.put(COL_INVENTORY_ITEM_QTY, item.getQuantity());

        // Perform an upsert (Insert or Replace) into the inventory descriptor table
        db.insertWithOnConflict(TABLE_INVENTORY, null, inventoryValues, SQLiteDatabase.CONFLICT_REPLACE);

        // Cross-Reference Location Table Update Payload
        ContentValues locationMappingValues = new ContentValues();
        locationMappingValues.put(COL_INV_LOC_INV_ID, item.id);              // The dynamic Item UUID
        locationMappingValues.put(COL_INV_LOC_LOC_ID, item.getLocationId());  // The dynamic Location UUID!
        locationMappingValues.put(COL_INV_LOC_QUANTITY, item.getQuantity());

        // Perform an upsert into your relational facility location index cross-reference table
        db.insertWithOnConflict(TABLE_INVENTORY_LOCATIONS, null, locationMappingValues, SQLiteDatabase.CONFLICT_REPLACE);

        db.close();
        Log.d("DatabaseHelper", "Successfully upserted item: " + item.getName() + " for Location: " + item.getLocationId());
    }

    public String getLocationIdByInventoryId(String inventoryId) {
        String foundLocationId = null;
        SQLiteDatabase db = this.getReadableDatabase();

        // Assumes your table/column layout references match your existing cross-reference sync layouts
        Cursor cursor = db.rawQuery("SELECT location_id FROM Inventory_Locations WHERE inventory_id = ?", new String[]{inventoryId});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                foundLocationId = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return foundLocationId;
    }

    public int getLowStockCount(String locationId, int defaultThreshold) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        // Build raw SQL query joining core rules with site stock quantities
        String rawQuery = "SELECT COUNT(*) FROM " + TABLE_INVENTORY + " i "
                + "INNER JOIN " + TABLE_INVENTORY_LOCATIONS + " il "
                + "ON i." + COL_INVENTORY_ITEM_ID + " = il." + COL_INV_LOC_INV_ID
                + " WHERE il." + COL_INV_LOC_LOC_ID + " = ? "
                + " AND il." + COL_INV_LOC_QUANTITY + " < COALESCE(i." + COL_INVENTORY_REORDER_POINT + ", ?)";

        Cursor cursor = db.rawQuery(rawQuery, new String[]{locationId, String.valueOf(defaultThreshold)});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }


    /**
     * Resets the is_dirty flag to 0 for a list of successfully synchronized row IDs.
     */
    public void clearLocalDirtyFlags(String tableName, List<String> idList) {
        android.database.sqlite.SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("is_dirty", 0); // Flip flag clean
            values.put("updated_at", String.valueOf(System.currentTimeMillis()));

            for (String id : idList) {
                db.update(tableName, values, "id = ?", new String[]{id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
