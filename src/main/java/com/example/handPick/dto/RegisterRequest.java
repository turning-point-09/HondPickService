package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    // At least one letter, one digit, one symbol, min 6, max 10
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,10}$",
            message = "Password must be 6-10 characters long, include letters, numbers, and symbols."
    )
    private String password;

    @Email(message = "Email should be valid")
    private String email; // Optional

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName; // Optional

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName; // Optional

    // Address fields (all optional)
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}