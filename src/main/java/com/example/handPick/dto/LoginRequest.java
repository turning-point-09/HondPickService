package com.example.handPick.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    private String password;
}