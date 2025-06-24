package com.example.handPick.dto; // UPDATED

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long productId;
    private int quantity;
}