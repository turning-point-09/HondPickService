package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // New: First Name and Last Name
    @Column(nullable = true, length = 50)
    private String firstName;

    @Column(nullable = true, length = 50)
    private String lastName;

    // New: Mobile Number
    @Column(nullable = true, unique = true, length = 15)
    private String mobileNumber;

    // One-to-one relationship with Address
    // CascadeType.ALL ensures Address is saved/deleted with User
    // orphanRemoval = true ensures Address is deleted if its User reference is nullified
    // Address is now optional during registration, so it can be null.
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address; // User's primary address

    // Removed: Set<Role> roles field as per your instruction "i donot want role"
}
