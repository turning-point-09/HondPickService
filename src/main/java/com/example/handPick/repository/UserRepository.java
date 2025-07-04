package com.example.handPick.repository;

import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email); // New: Check if email exists
    boolean existsByMobileNumber(String mobileNumber); // New: Check if mobile number exists

    // Fetch user with their address eagerly by ID
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.id = :id")
    Optional<User> findByIdWithAddress(Long id);

    // Fetch user with their address eagerly by username
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.username = :username")
    Optional<User> findByUsernameWithAddress(String username);

    // Fetch user with their address and mobile number eagerly by ID (if needed for profile)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.id = :id")
    Optional<User> findByIdWithAddressAndMobileNumber(Long id);

    // Fetch user with their address and mobile number eagerly by username (if needed for profile)
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.username = :username")
    Optional<User> findByUsernameWithAddressAndMobileNumber(String username);
}
