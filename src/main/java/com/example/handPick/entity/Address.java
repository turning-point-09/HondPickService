package com.example.handPick.entity; // Adjusted package name

import lombok.Data;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+

@Entity
@Table(name = "address") // Maps to the 'address' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "zip_code")
    private String zipCode;

    // Removed the @OneToOne @PrimaryKeyJoinColumn for 'order'
    // as it conflicts with Order's @JoinColumn mapping.
    // Address is now a standalone entity referenced by foreign keys.
    // private Order order;
}