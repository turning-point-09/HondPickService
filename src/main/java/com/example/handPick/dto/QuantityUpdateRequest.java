package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuantityUpdateRequest {
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity; // The new quantity for the cart item
}