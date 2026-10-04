package com.example.peck_project;

public class LocationModel {
    //Private fields matching your database columns
    private String id;
    private String userId;
    private String locationName;
    private String subLocation;
    private String createdAt; // Can also be java.util.Date depending on your parsing

    // Empty constructor (required by many databases/libraries like Firebase or Room)
    public LocationModel() {
    }

    // Constructor with all fields (useful for creating new instances)
    public LocationModel(String id, String userId, String locationName, String subLocation, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.locationName = locationName;
        this.subLocation = subLocation;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getSubLocation() { return subLocation; }
    public void setSubLocation(String subLocation) { this.subLocation = subLocation; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
