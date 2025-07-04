package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private Double unitPrice; // Using Double for frontend display, BigDecimal for backend calculations
    private Integer quantity;
    private Double subtotal;  // Calculated (unitPrice * quantity)
    private String imageUrl;

    // New: Selected size/unit for the product
    private String size;
}
