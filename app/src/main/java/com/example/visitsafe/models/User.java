package com.example.visitsafe.models;

public class User {
    private String userId;
    private String name;
    private String phone;
    private String role; // "admin", "resident", or "security"
    private String flatNumber;
    private String fcmToken;
    private String password;
    private String apartmentId;

    public User() {} // Required for Firestore

    public User(String userId, String name, String phone, String role, String flatNumber, String password) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.flatNumber = flatNumber;
        this.password = password;
    }

    public User(String userId, String name, String phone, String role, String flatNumber, String password, String apartmentId) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.flatNumber = flatNumber;
        this.password = password;
        this.apartmentId = apartmentId;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }
}