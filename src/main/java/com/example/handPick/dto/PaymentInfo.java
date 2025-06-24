package com.example.handPick.dto; // Adjusted package name

import lombok.Data;

@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class PaymentInfo {

    private int amount;
    private String currency;
    private String receiptEmail;

}