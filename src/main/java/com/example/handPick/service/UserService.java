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
    private AddressRepository addressRepository; // Still needed for address management

    // Removed: @Autowired private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user with provided details. Address, first name, last name, and mobile number are now optional.
     * @param username The desired username.
     * @param password The raw password.
     * @param email The user's email.
     * @param firstName The user's first name (optional).
     * @param lastName The user's last name (optional).
     * @param mobileNumber The user's mobile number (optional).
     * @param street The street address (optional).
     * @param city The city (optional).
     * @param state The state (optional).
     * @param postalCode The postal code (optional).
     * @param country The country (optional).
     * @return The newly registered User entity.
     * @throws RuntimeException if the username, email, or mobile number already exists.
     */
    @Transactional
    public User registerUser(String username, String password, String email,
                             String firstName, String lastName, String mobileNumber,
                             String street, String city, String state, String postalCode, String country) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered!");
        }
        if (mobileNumber != null && !mobileNumber.isEmpty() && userRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Mobile number already registered!");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // Encode the password
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setMobileNumber(mobileNumber);

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
            newUser.setAddress(newAddress); // Set the address on the user
        }

        // Removed: Role assignment logic

        // Save the user (Address will be cascaded and saved automatically if present)
        return userRepository.save(newUser);
    }

    /**
     * Finds a user by username, eagerly fetching their address.
     * @param username The username to search for.
     * @return An Optional containing the User if found.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameWithAddress(username);
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
            // Create a new address if the user doesn't have one
            address = new Address();
            user.setAddress(address); // Link the new address to the user
        }

        // Update address fields from DTO
        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setPostalCode(addressDto.getPostalCode());
        address.setCountry(addressDto.getCountry());

        // Save the user to cascade the address changes (or save address directly if not cascaded)
        userRepository.save(user); // Saving user will cascade persist/update to address
        return address;
    }
}
