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
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    // Regex for at least one letter, one digit, one symbol, min 6, max 10
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{6,10}$",
            message = "Password must be 6-10 characters long, include letters, numbers, and symbols.")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    // New: First Name and Last Name (optional for registration, but good practice to collect)
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    // New: Mobile Number (optional for registration)
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") // Example: 10 digits only
    private String mobileNumber;

    // Address fields for registration - now optional
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}