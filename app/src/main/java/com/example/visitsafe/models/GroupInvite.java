package com.example.visitsafe.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class GroupInvite {
    private String inviteId;
    private String createdBy; // phone
    private String apartmentId;
    private String flatNumber;
    private String groupName;
    private int totalVisitors;
    private String purpose;
    private String status; // PENDING, ENTERED, EXITED
    
    @ServerTimestamp
    private Timestamp createdAt;
    private Timestamp entryTime;
    private Timestamp exitTime;

    public GroupInvite() {}

    public GroupInvite(String inviteId, String createdBy, String apartmentId, String flatNumber, String groupName, int totalVisitors, String purpose) {
        this.inviteId = inviteId;
        this.createdBy = createdBy;
        this.apartmentId = apartmentId;
        this.flatNumber = flatNumber;
        this.groupName = groupName;
        this.totalVisitors = totalVisitors;
        this.purpose = purpose;
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public int getTotalVisitors() { return totalVisitors; }
    public void setTotalVisitors(int totalVisitors) { this.totalVisitors = totalVisitors; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getEntryTime() { return entryTime; }
    public void setEntryTime(Timestamp entryTime) { this.entryTime = entryTime; }
    public Timestamp getExitTime() { return exitTime; }
    public void setExitTime(Timestamp exitTime) { this.exitTime = exitTime; }
}