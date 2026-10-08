package com.example.peck_project;

public class InventoryItemModel {
    final private String id;
    final private String name;
    final private String sku;
    final private int quantity;
    private String locationId;

    // Constructor to instantiate the model from database query results
    public InventoryItemModel(String id, String name, String sku, int quantity) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.quantity = quantity;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
}
