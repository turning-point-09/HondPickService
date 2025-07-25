package com.example.handPick.service;

import com.example.handPick.dto.AddressDto;
import com.example.handPick.model.Address;
import com.example.handPick.model.User;
import com.example.handPick.repository.AddressRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with provided details. Address, first name, last name, and email are optional.
     * @param mobileNumber The user's mobile number (required, unique).
     * @param password The raw password.
     * @param email The user's email (optional, unique if provided).
     * @param firstName The user's first name (optional).
     * @param lastName The user's last name (optional).
     * @param street The street address (optional).
     * @param city The city (optional).
     * @param state The state (optional).
     * @param postalCode The postal code (optional).
     * @param country The country (optional).
     * @return The newly registered User entity.
     * @throws RuntimeException if the mobile number or email already exists.
     */
    @Transactional
    public User registerUser(String mobileNumber, String password, String email,
                             String firstName, String lastName,
                             String street, String city, String state, String postalCode, String country) {
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Mobile number already registered!");
        }
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered!");
        }

        User newUser = new User();
        newUser.setMobileNumber(mobileNumber);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setRole("USER");
        newUser.setUsername(mobileNumber); // or use email, or a new field

        // Handle optional Address
        if (street != null && !street.isEmpty() &&
                city != null && !city.isEmpty() &&
                state != null && !state.isEmpty() &&
                postalCode != null && !postalCode.isEmpty() &&
                country != null && !country.isEmpty()) {
            Address newAddress = new Address();
            newAddress.setStreet(street);
            newAddress.setCity(city);
            newAddress.setState(state);
            newAddress.setPostalCode(postalCode);
            newAddress.setCountry(country);
            newAddress.setUser(newUser); // Ensure user_id is set
            newUser.setAddress(newAddress);
        }

        return userRepository.save(newUser);
    }

    /**
     * Finds a user by mobile number, eagerly fetching their address.
     * @param mobileNumber The mobile number to search for.
     * @return An Optional containing the User if found.
     */
    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumberWithAddress(mobileNumber);
    }

    /**
     * Finds a user by ID, eagerly fetching their address.
     * @param id The user ID to search for.
     * @return An Optional containing the User if found.
     */
    public Optional<User> findById(Long id) {
        return userRepository.findByIdWithAddress(id);
    }

    /**
     * Updates a user's address. If the user doesn't have an address, a new one is created.
     * If an address exists, it's updated.
     * @param userId The ID of the user whose address to update.
     * @param addressDto The DTO containing the new address details.
     * @return The updated Address entity.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public Address updateOrCreateUserAddress(Long userId, AddressDto addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Address address = user.getAddress();
        if (address == null) {
            address = new Address();
            user.setAddress(address);
        }

        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry());

        userRepository.save(user);
        return address;
    }
}