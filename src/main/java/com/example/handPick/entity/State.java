package com.example.handPick.entity; // Adjusted package name

import lombok.Data;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+

@Entity
@Table(name = "state") // Maps to the 'state' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    // Defines a many-to-one relationship with the Country entity.
    // @JoinColumn specifies the foreign key column in the 'state' table that
    // references the 'country' table.
    @ManyToOne
    @JoinColumn(name = "country_id") // Foreign key column
    private Country country; // The Country this state belongs to
}