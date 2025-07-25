package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {
    private Long id;
    private List<CartItemDto> items = new ArrayList<>();
    private BigDecimal subtotalBeforeTax = BigDecimal.ZERO; // Subtotal before tax
    private BigDecimal gstAmount = BigDecimal.ZERO;         // Calculated GST amount
    private BigDecimal totalAmount = BigDecimal.ZERO;       // Total including GST
    private int totalItems;
    private Long userId; // For authenticated users
    private String status; // e.g., "ACTIVE", "ORDERED"
}