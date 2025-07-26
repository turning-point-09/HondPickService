package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.handPick.model.Order.PaymentMethod; // Import the enum

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod; // COD, GPAY, PHONEPE

    // Address selection options
    private Long selectedAddressId; // ID of existing address to use
    private AddressDto newAddress; // New address details if adding new address
    
    // Address saving options
    private String addressLabel; // Label for the new address (e.g., "Home", "Office")
    private String saveAddressAs; // "PERMANENT", "TEMPORARY", "ONE_TIME", or null for no saving
    private boolean setAsDefault = false; // Whether to set as default address
}
