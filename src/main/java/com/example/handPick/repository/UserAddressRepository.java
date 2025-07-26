package com.example.handPick.repository;

import com.example.handPick.model.User;
import com.example.handPick.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    
    // Find all addresses for a user
    List<UserAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    // Find default address for a user
    Optional<UserAddress> findByUserAndIsDefaultTrue(User user);
    
    // Find addresses by type for a user
    List<UserAddress> findByUserAndAddressTypeOrderByCreatedAtDesc(User user, UserAddress.AddressType addressType);
    
    // Find permanent addresses for a user
    List<UserAddress> findByUserAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(User user, UserAddress.AddressType addressType);
    
    // Find temporary addresses that are older than specified date (for cleanup)
    @Query("SELECT ua FROM UserAddress ua WHERE ua.addressType = 'TEMPORARY' AND ua.createdAt < :cutoffDate")
    List<UserAddress> findOldTemporaryAddresses(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Count addresses for a user
    long countByUser(User user);
} 