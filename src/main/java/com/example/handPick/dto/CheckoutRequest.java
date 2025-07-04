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

    // New: Optional AddressDto for updating/providing shipping address at checkout
    private AddressDto shippingAddress;
}
