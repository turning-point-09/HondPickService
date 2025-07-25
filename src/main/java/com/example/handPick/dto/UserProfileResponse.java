package com.example.handPick.dto;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String mobileNumber;

    public UserProfileResponse(Long id, String username, String email, String role, String mobileNumber) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.mobileNumber = mobileNumber;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getMobileNumber() { return mobileNumber; }
} 