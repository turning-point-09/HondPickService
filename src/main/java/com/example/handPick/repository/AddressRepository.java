package com.example.handPick.repository;

import com.example.handPick.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    // You can add custom query methods here if needed,
    // but JpaRepository provides basic CRUD operations.
}
