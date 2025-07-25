package com.example.handPick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class to store shipping address details directly within the Order entity.
 * This ensures that the address associated with an order is a snapshot at the time of order placement,
 * independent of any future changes to the user's primary address.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "shipping_street", length = 100)
    private String street;

    @Column(name = "shipping_city", length = 50)
    private String city;

    @Column(name = "shipping_state", length = 50)
    private String state;

    @Column(name = "shipping_postal_code", length = 10)
    private String postalCode;

    @Column(name = "shipping_country", length = 50)
    private String country;
}