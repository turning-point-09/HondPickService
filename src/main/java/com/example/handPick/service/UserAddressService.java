package com.example.handPick.service;

import com.example.handPick.dto.UserAddressDto;
import com.example.handPick.model.User;
import com.example.handPick.model.UserAddress;
import com.example.handPick.repository.UserAddressRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserAddressService {

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all addresses for a user
     */
    public List<UserAddressDto> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return userAddressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get default address for a user
     */
    public Optional<UserAddressDto> getDefaultAddress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return userAddressRepository.findByUserAndIsDefaultTrue(user)
                .map(this::convertToDto);
    }

    /**
     * Get address by ID
     */
    public UserAddressDto getAddressById(Long addressId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this address");
        }
        
        return convertToDto(address);
    }

    /**
     * Add new address for user
     */
    @Transactional
    public UserAddressDto addAddress(Long userId, UserAddressDto addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If setting as default, unset other default addresses
        if (addressDto.isDefault()) {
            userAddressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        userAddressRepository.save(existingDefault);
                    });
        }

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry());
        address.setAddressLabel(addressDto.getAddressLabel());
        address.setAddressType(UserAddress.AddressType.valueOf(addressDto.getAddressType()));
        address.setDefault(addressDto.isDefault());

        UserAddress savedAddress = userAddressRepository.save(address);
        return convertToDto(savedAddress);
    }

    /**
     * Update existing address
     */
    @Transactional
    public UserAddressDto updateAddress(Long addressId, Long userId, UserAddressDto addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this address");
        }

        // If setting as default, unset other default addresses
        if (addressDto.isDefault() && !address.isDefault()) {
            userAddressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existingDefault -> {
                        existingDefault.setDefault(false);
                        userAddressRepository.save(existingDefault);
                    });
        }

        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry());
        address.setAddressLabel(addressDto.getAddressLabel());
        address.setAddressType(UserAddress.AddressType.valueOf(addressDto.getAddressType()));
        address.setDefault(addressDto.isDefault());

        UserAddress updatedAddress = userAddressRepository.save(address);
        return convertToDto(updatedAddress);
    }

    /**
     * Delete address
     */
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this address");
        }

        userAddressRepository.delete(address);
    }

    /**
     * Set address as default
     */
    @Transactional
    public UserAddressDto setDefaultAddress(Long addressId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this address");
        }

        // Unset current default
        userAddressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    userAddressRepository.save(existingDefault);
                });

        // Set new default
        address.setDefault(true);
        UserAddress updatedAddress = userAddressRepository.save(address);
        return convertToDto(updatedAddress);
    }

    /**
     * Clean up old temporary addresses (older than 30 days)
     */
    @Transactional
    public void cleanupOldTemporaryAddresses() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<UserAddress> oldTemporaryAddresses = userAddressRepository.findOldTemporaryAddresses(cutoffDate);
        userAddressRepository.deleteAll(oldTemporaryAddresses);
    }

    /**
     * Convert UserAddress entity to DTO
     */
    private UserAddressDto convertToDto(UserAddress address) {
        return new UserAddressDto(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getAddressLabel(),
                address.getAddressType().name(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
} 