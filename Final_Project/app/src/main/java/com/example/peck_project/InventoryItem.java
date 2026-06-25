package com.example.peck_project;

public class InventoryItem {
    private String name;
    private String itemNumber;
    private int quantity;
    private String description;
    private byte[] imageBytes;

    public InventoryItem(String name, String itemNumber, int quantity, String description, byte[] imageBytes) {
        this.name = name;
        this.itemNumber = itemNumber;
        this.quantity = quantity;
        this.description = description;
        this.imageBytes = imageBytes;
    }

    public String getName() { return name; }
    public String getItemNumber() { return itemNumber; }
    public int getQuantity() { return quantity; }
    public String getDescription() { return description; }
    public byte[] getImageBytes() { return imageBytes; }
}
