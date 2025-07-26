package com.example.handPick.dto;

public class UserStatusDto {
    private Long userId;
    private Boolean active;
    private String message;

    // Default constructor
    public UserStatusDto() {}

    // Constructor with fields
    public UserStatusDto(Long userId, Boolean active, String message) {
        this.userId = userId;
        this.active = active;
        this.message = message;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 