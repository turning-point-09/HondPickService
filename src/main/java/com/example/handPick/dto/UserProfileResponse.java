package com.example.handPick.dto;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String mobileNumber;


    public UserProfileResponse(Long id, String username, String firstName, String lastName, String email, String role, String mobileNumber) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.mobileNumber = mobileNumber;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getMobileNumber() { return mobileNumber; }
} 