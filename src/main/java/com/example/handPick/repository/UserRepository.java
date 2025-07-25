package com.example.handPick.repository;

import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobileNumber(String mobileNumber);
    boolean existsByMobileNumber(String mobileNumber);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    // Fetch user with their address eagerly by ID
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.id = :id")
    Optional<User> findByIdWithAddress(Long id);

    // Fetch user with their address eagerly by mobile number
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.mobileNumber = :mobileNumber")
    Optional<User> findByMobileNumberWithAddress(String mobileNumber);
}