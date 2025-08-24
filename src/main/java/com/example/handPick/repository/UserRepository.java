package com.example.handPick.repository;

import com.example.handPick.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    boolean existsByUsername(String username);

    // Fetch user with their address eagerly by ID
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.id = :id")
    Optional<User> findByIdWithAddress(Long id);

    // Fetch user with their address eagerly by mobile number
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.mobileNumber = :mobileNumber")
    Optional<User> findByMobileNumberWithAddress(String mobileNumber);

    // Find users by active status
    Page<User> findByActiveTrue(Pageable pageable);
    Page<User> findByActiveFalse(Pageable pageable);
}