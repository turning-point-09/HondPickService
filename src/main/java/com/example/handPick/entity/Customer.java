package com.example.handPick.entity; // Adjusted package name

import lombok.Data;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customer") // Maps to the 'customer' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    // Defines a one-to-many relationship with the Order entity.
    // 'mappedBy = "customer"' indicates that the 'customer' field in the Order entity
    // is the owning side of this relationship.
    // CascadeType.ALL means that persistence operations (like persist, merge, remove)
    // applied to Customer will cascade to its associated Order entities.
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Order> orders = new HashSet<>(); // Initialize to prevent NullPointerException

    // Helper method to add an Order to the customer's set of orders
    // and establish the bidirectional relationship.
    public void add(Order order) {
        if (order != null) {
            if (orders == null) {
                orders = new HashSet<>(); // Ensure the set is initialized
            }
            orders.add(order);
            order.setCustomer(this); // Set the customer on the order side of the relationship
        }
    }
}