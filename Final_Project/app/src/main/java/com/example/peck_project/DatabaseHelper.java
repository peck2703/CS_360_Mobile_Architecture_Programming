package com.example.peck_project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Logins.db";
    private static final int DATABASE_VERSION = 3; // Version 3 handles users, inventory, and audits

    // Table: Users
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_FIRST_NAME = "first_name";
    private static final String COL_LAST_NAME = "last_name";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    // Table: Inventory
    private static final String TABLE_INVENTORY = "inventory";
    private static final String COL_ITEM_ID = "id";
    private static final String COL_ITEM_NAME = "item_name";
    private static final String COL_ITEM_NUMBER = "item_number";
    private static final String COL_ITEM_QTY = "item_quantity";
    private static final String COL_ITEM_DESC = "item_description";
    private static final String COL_ITEM_IMAGE = "item_image";

    // Table: Discrepancies
    private static final String TABLE_AUDITS = "discrepancies";
    private static final String COL_AUDIT_ID = "id";
    private static final String COL_AUDIT_ITEM_NUM = "item_number";
    private static final String COL_AUDIT_TYPE = "discrepancy_type";
    private static final String COL_AUDIT_COUNT = "reported_count";
    private static final String COL_AUDIT_DESC = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FIRST_NAME + " TEXT, " +
                COL_LAST_NAME + " TEXT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        // Create Inventory Table
        String createInventoryTable = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_NAME + " TEXT, " +
                COL_ITEM_NUMBER + " TEXT UNIQUE, " +
                COL_ITEM_QTY + " INTEGER, " +
                COL_ITEM_DESC + " TEXT, " +
                COL_ITEM_IMAGE + " BLOB)";
        db.execSQL(createInventoryTable);

        // Create Audits Table
        String createAuditsTable = "CREATE TABLE " + TABLE_AUDITS + " (" +
                COL_AUDIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AUDIT_ITEM_NUM + " TEXT, " +
                COL_AUDIT_TYPE + " TEXT, " +
                COL_AUDIT_COUNT + " INTEGER, " +
                COL_AUDIT_DESC + " TEXT)";
        db.execSQL(createAuditsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDITS);
        onCreate(db);
    }

    // USER OPERATIONS
    public boolean registerUser(String firstName, String lastName, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_FIRST_NAME, firstName);
        contentValues.put(COL_LAST_NAME, lastName);
        contentValues.put(COL_EMAIL, email);
        contentValues.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    public boolean checkUserCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    // INVENTORY OPERATIONS
    public boolean addItem(String name, String itemNumber, int quantity, String description, byte[] imageBytes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_ITEM_NAME, name);
        contentValues.put(COL_ITEM_NUMBER, itemNumber);
        contentValues.put(COL_ITEM_QTY, quantity);
        contentValues.put(COL_ITEM_DESC, description);
        contentValues.put(COL_ITEM_IMAGE, imageBytes);
        long result = db.insert(TABLE_INVENTORY, null, contentValues);
        return result != -1;
    }

    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_INVENTORY, null, null, null, null, null, null);
    }

    public boolean updateItemQuantity(String itemNumber, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_ITEM_QTY, newQuantity);
        int rowsAffected = db.update(TABLE_INVENTORY, contentValues, COL_ITEM_NUMBER + " = ?", new String[]{itemNumber});
        return rowsAffected > 0;
    }

    public boolean deleteItem(String itemNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_INVENTORY, COL_ITEM_NUMBER + " = ?", new String[]{itemNumber});
        return rowsDeleted > 0;
    }

    // AUDIT OPERATIONS
    public boolean logDiscrepancy(String itemNumber, String type, int count, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_AUDIT_ITEM_NUM, itemNumber);
        contentValues.put(COL_AUDIT_TYPE, type);
        contentValues.put(COL_AUDIT_COUNT, count);
        contentValues.put(COL_AUDIT_DESC, notes);
        long result = db.insert(TABLE_AUDITS, null, contentValues);
        return result != -1;
    }

    // Place this inside your LoginDatabaseHelper.java class file

    public int getLowStockCount(int threshold) {
        SQLiteDatabase db = this.getReadableDatabase();

        // SQL query counting rows where the item quantity falls below the threshold
        String query = "SELECT COUNT(*) FROM inventory WHERE item_quantity < ?";
        String[] selectionArgs = { String.valueOf(threshold) };

        Cursor cursor = db.rawQuery(query, selectionArgs);
        int count = 0;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0); // Extract the numerical count from the first column
            }
            cursor.close(); // Always close cursors to prevent memory leaks
        }
        return count;
    }

}
