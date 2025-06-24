package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    private Long orderId;
    private String message;
    private boolean success;
}