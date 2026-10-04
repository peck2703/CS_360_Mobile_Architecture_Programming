package com.example.peck_project;

public class AuditModel {
    private String auditsId;
    private String userId;
    private String auditsInvId;
    private String auditsLocId;
    private int auditsExpected;
    private int auditsActual;
    private String auditsStatus;
    private String auditsNotes;
    private String auditsCreated;
    private String auditsResolvedAt;
    private int auditsDiscrepancy;

    // Empty Constructor
    public AuditModel() {}

    // Full Constructor
    public AuditModel(String auditsId, String userId, String auditsInvId, String auditsLocId,
                      int auditsExpected, int auditsActual, String auditsStatus,
                      String auditsNotes, String auditsCreated, String auditsResolvedAt,
                      int auditsDiscrepancy) {
        this.auditsId = auditsId;
        this.userId = userId;
        this.auditsInvId = auditsInvId;
        this.auditsLocId = auditsLocId;
        this.auditsExpected = auditsExpected;
        this.auditsActual = auditsActual;
        this.auditsStatus = auditsStatus;
        this.auditsNotes = auditsNotes;
        this.auditsCreated = auditsCreated;
        this.auditsResolvedAt = auditsResolvedAt;
        this.auditsDiscrepancy = auditsDiscrepancy;
    }

    // Getters and Setters
    public String getAuditsId() { return auditsId; }
    public void setAuditsId(String auditsId) { this.auditsId = auditsId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAuditsInvId() { return auditsInvId; }
    public void setAuditsInvId(String auditsInvId) { this.auditsInvId = auditsInvId; }

    public String getAuditsLocId() { return auditsLocId; }
    public void setAuditsLocId(String auditsLocId) { this.auditsLocId = auditsLocId; }

    public int getAuditsExpected() { return auditsExpected; }
    public void setAuditsExpected(int auditsExpected) { this.auditsExpected = auditsExpected; }

    public int getAuditsActual() { return auditsActual; }
    public void setAuditsActual(int auditsActual) { this.auditsActual = auditsActual; }

    public String getAuditsStatus() { return auditsStatus; }
    public void setAuditsStatus(String auditsStatus) { this.auditsStatus = auditsStatus; }

    public String getAuditsNotes() { return auditsNotes; }
    public void setAuditsNotes(String auditsNotes) { this.auditsNotes = auditsNotes; }

    public String getAuditsCreated() { return auditsCreated; }
    public void setAuditsCreated(String auditsCreated) { this.auditsCreated = auditsCreated; }

    public String getAuditsResolvedAt() { return auditsResolvedAt; }
    public void setAuditsResolvedAt(String auditsResolvedAt) { this.auditsResolvedAt = auditsResolvedAt; }

    public int getAuditsDiscrepancy() { return auditsDiscrepancy; }
    public void setAuditsDiscrepancy(int auditsDiscrepancy) { this.auditsDiscrepancy = auditsDiscrepancy; }
}
