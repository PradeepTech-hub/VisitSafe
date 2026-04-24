package com.example.visitsafe.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Invite {
    private String inviteId;
    private String apartmentId;
    private String role; // "resident" or "security"
    private String createdBy; // admin phone or resident UID
    private String visitorName;
    private String flatNumber;
    private boolean isUsed;
    
    @ServerTimestamp
    private Timestamp createdAt;
    private Timestamp validTill;

    public Invite() {}

    // Constructor for Admin Join Invites
    public Invite(String inviteId, String apartmentId, String role, String createdBy) {
        this.inviteId = inviteId;
        this.apartmentId = apartmentId;
        this.role = role;
        this.createdBy = createdBy;
        this.isUsed = false;
    }

    // Constructor for Resident Visitor QR Invites
    public Invite(String inviteId, String createdBy, String visitorName, String flatNumber, Timestamp validTill) {
        this.inviteId = inviteId;
        this.createdBy = createdBy;
        this.visitorName = visitorName;
        this.flatNumber = flatNumber;
        this.validTill = validTill;
        this.isUsed = false;
    }

    // Getters and Setters
    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getValidTill() { return validTill; }
    public void setValidTill(Timestamp validTill) { this.validTill = validTill; }
}