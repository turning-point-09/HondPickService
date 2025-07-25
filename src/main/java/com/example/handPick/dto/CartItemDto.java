package com.example.handPick.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;   // Use BigDecimal for price accuracy
    private Integer quantity;
    private BigDecimal subtotal;    // Calculated (unitPrice * quantity)
    private String imageUrl;
    private String size;            // Selected size/unit for the product
}