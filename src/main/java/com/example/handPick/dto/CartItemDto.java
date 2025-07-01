package com.example.handPick.dto; // Corrected package name to handPick

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// No direct BigDecimal import needed if fields are Double, but kept for context if desired.
// import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    // FIX: Changed unitPrice to Double to match frontend (number) and CartService conversion
    private Double unitPrice;
    private Integer quantity;
    // FIX: Changed subtotal to Double to match frontend (number) and CartService conversion
    private Double subtotal;
    private String imageUrl; // Optional
}
