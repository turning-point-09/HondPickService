package com.example.handPick.dto; // UPDATED

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
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private String status; // e.g., "ACTIVE", "ORDERED"
}