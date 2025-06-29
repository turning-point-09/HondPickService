package com.example.handPick.dto; // UPDATED

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private Long id;
    private Long productId; // Assuming a Product entity exists
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String imageUrl; // Optional

}

//@Data
//public class CartItemDto {
//    private Long productId;
//    private String productName;
//    private BigDecimal price;
//    private int quantity;
//    private String imageUrl; // Optional
//}