package com.example.peck_project;

public class InventoryItemModel {
    private String id;
    private String name;
    private String sku;
    private int quantity;

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
}
