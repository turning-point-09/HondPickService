package com.example.handPick.dto; // Adjusted package name

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor // Lombok annotation to generate a constructor with all fields
@Data               // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class PurchaseResponse {

    private String orderTrackingNumber;

}