package com.example.handPick.dao; // Adjusted package name

import com.example.handPick.entity.Customer; // Adjusted import for Customer entity
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Custom query method: Spring Data JPA will automatically generate the query
    // to find a Customer based on their email address.
    Customer findByEmail(String email);
}