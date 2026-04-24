package com.example.visitsafe.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Visitor {
    private String visitorId;
    private String name;
    private String phone;
    private String flatNumber;
    private String purpose;
    private String status; // "pending", "approved", "rejected", "entered", "exited"
    private String otp;
    private String securityId;
    private String apartmentId;
    
    @ServerTimestamp
    private Timestamp createdAt;
    private Timestamp approvedAt;
    private Timestamp entryTime;
    private Timestamp exitTime;

    public Visitor() {}

    public Visitor(String name, String phone, String flatNumber, String purpose, String securityId) {
        this.name = name;
        this.phone = phone;
        this.flatNumber = flatNumber;
        this.purpose = purpose;
        this.securityId = securityId;
        this.status = "pending";
    }

    // Getters and Setters
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Timestamp approvedAt) { this.approvedAt = approvedAt; }
    public Timestamp getEntryTime() { return entryTime; }
    public void setEntryTime(Timestamp entryTime) { this.entryTime = entryTime; }
    public Timestamp getExitTime() { return exitTime; }
    public void setExitTime(Timestamp exitTime) { this.exitTime = exitTime; }
}