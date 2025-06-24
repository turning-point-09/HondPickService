package com.example.handPick.dto; // UPDATED

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}