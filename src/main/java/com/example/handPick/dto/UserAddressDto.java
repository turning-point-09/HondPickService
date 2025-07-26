package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDto {
    private Long id;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String addressLabel;
    private String addressType; // PERMANENT, TEMPORARY, ONE_TIME
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 