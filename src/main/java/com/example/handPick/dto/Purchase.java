package com.example.handPick.dto; // Adjusted package name

import com.example.handPick.entity.Address;    // Adjusted import
import com.example.handPick.entity.Customer;   // Adjusted import
import com.example.handPick.entity.Order;      // Adjusted import
import com.example.handPick.entity.OrderItem;  // Adjusted import
import lombok.Data;

import java.util.Set;

@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class Purchase {

    private Customer customer;

    private Address shippingAddress;

    private Address billingAddress;

    private Order order;

    private Set<OrderItem> orderItems;
}