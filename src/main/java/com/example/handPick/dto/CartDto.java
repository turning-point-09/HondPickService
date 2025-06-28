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
    private int totalItems;
    private Long userId; // For authenticated users

    private String status; // e.g., "ACTIVE", "ORDERED"
}

//@Data
//public class CartDto {
//    private Long id; // For persisted carts
//    private BigDecimal totalPrice;
//    private int totalItems;
//    private List<CartItemDto> items;
//    private UUID guestId; // UUID for guest sessions
//    private Long userId; // For authenticated users
//}