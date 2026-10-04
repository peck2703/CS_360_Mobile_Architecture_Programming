package com.example.peck_project;

import com.google.gson.annotations.SerializedName;

public class InventoryItem {
    @SerializedName("id") public String id;
    @SerializedName("user_id") public String userId;
    @SerializedName("sku") public String sku;
    @SerializedName("item_name") public String itemName;
    @SerializedName("item_description") public String itemDescription;
    @SerializedName("item_quantity") public int itemQuantity;
    @SerializedName("cost_price") public double costPrice;
    @SerializedName("retail_price") public double retailPrice;
    @SerializedName("reorder_point") public int reorderPoint;
    @SerializedName("item_image") public String itemImage; // Cloud uses a URL string to your bucket
    @SerializedName("created_at") public String createdAt;
    @SerializedName("updated_at") public String updatedAt;

    // Default constructor required by Gson for JSON parsing
    public InventoryItem() {}

    // Updated constructor mapping your UI parameters directly to the database fields
    public InventoryItem(String name, String itemNumber, int quantity, String description, String itemImageUrl) {
        this.itemName = name;
        this.sku = itemNumber; // Maps itemNumber to your database SKU
        this.itemQuantity = quantity;
        this.itemDescription = description;
        this.itemImage = itemImageUrl;
    }

    // Updated clean Getter implementations
    public String getName() { return itemName; }
    public String getItemNumber() { return sku; }
    public int getQuantity() { return itemQuantity; }
    public String getDescription() { return itemDescription; }
    public String getItemImage() { return itemImage; }

    /**
     * Serializes the active item into a clean JSON string matching your Supabase columns.
     */
    public String toJsonString() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("user_id", this.userId);
        json.addProperty("sku", this.sku);
        json.addProperty("item_name", this.itemName);
        json.addProperty("item_description", this.itemDescription);
        json.addProperty("item_quantity", this.itemQuantity);
        json.addProperty("cost_price", this.costPrice);
        json.addProperty("retail_price", this.retailPrice);
        json.addProperty("reorder_point", this.reorderPoint);
        json.addProperty("item_image", this.itemImage);
        return json.toString();
    }

}
