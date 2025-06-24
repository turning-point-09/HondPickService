package com.example.handPick.entity; // Adjusted package name

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.util.List; // Ensure java.util.List is imported

@Entity
@Table(name = "country") // Maps to the 'country' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private int id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    // Defines a one-to-many relationship with State entity.
    // 'mappedBy = "country"' indicates that the 'country' field in the State entity
    // is the owning side of this relationship.
    // @JsonIgnore prevents infinite recursion during JSON serialization, as
    // Country has States, and each State would have a Country, leading to a loop.
    @OneToMany(mappedBy = "country")
    @JsonIgnore
    private List<State> stateList;

}