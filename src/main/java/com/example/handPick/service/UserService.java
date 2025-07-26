package com.example.handPick.service;

import com.example.handPick.dto.AddressDto;
import com.example.handPick.dto.UserDto;
import com.example.handPick.dto.UserStatsDto;
import com.example.handPick.model.Address;
import com.example.handPick.model.User;
import com.example.handPick.model.UserAddress;
import com.example.handPick.repository.AddressRepository;
import com.example.handPick.repository.OrderRepository;
import com.example.handPick.repository.UserAddressRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private OrderRepository orderRepository;

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

        User savedUser = userRepository.save(newUser);

        // Also save as UserAddress for multi-address system
        if (street != null && !street.isEmpty() &&
                city != null && !city.isEmpty() &&
                state != null && !state.isEmpty() &&
                postalCode != null && !postalCode.isEmpty() &&
                country != null && !country.isEmpty()) {
            UserAddress userAddress = new UserAddress();
            userAddress.setUser(savedUser);
            userAddress.setStreet(street);
            userAddress.setCity(city);
            userAddress.setState(state);
            userAddress.setPostalCode(postalCode);
            userAddress.setCountry(country);
            userAddress.setAddressLabel("Home");
            userAddress.setAddressType(UserAddress.AddressType.PERMANENT);
            userAddress.setDefault(true);
            userAddressRepository.save(userAddress);
        }

        return savedUser;
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

    /**
     * Gets all users with pagination and converts them to DTOs.
     * @param pageable The pagination parameters.
     * @return A Page of UserDto.
     */
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDto);
    }

    /**
     * Gets all users and converts them to DTOs.
     * @return A list of UserDto.
     */
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a User entity to a UserDto.
     * @param user The User entity to convert.
     * @return The corresponding UserDto.
     */
    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setActive(user.getActive());
        
        // Convert address if it exists
        if (user.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            addressDto.setId(user.getAddress().getId());
            addressDto.setStreet(user.getAddress().getStreet());
            addressDto.setCity(user.getAddress().getCity());
            addressDto.setState(user.getAddress().getState());
            addressDto.setPostalCode(user.getAddress().getPostalCode());
            addressDto.setCountry(user.getAddress().getCountry());
            dto.setAddress(addressDto);
        }
        
        return dto;
    }

    /**
     * Activates a user account (admin only).
     * @param userId The ID of the user to activate.
     * @return The updated User entity.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public User activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setActive(true);
        return userRepository.save(user);
    }

    /**
     * Deactivates a user account (admin only).
     * @param userId The ID of the user to deactivate.
     * @return The updated User entity.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setActive(false);
        return userRepository.save(user);
    }

    /**
     * Allows a user to delete their own account (soft delete - sets active to false).
     * @param userId The ID of the user deleting their account.
     * @return The updated User entity.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public User deleteUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setActive(false);
        return userRepository.save(user);
    }

    /**
     * Gets all active users with pagination.
     * @param pageable The pagination parameters.
     * @return A Page of UserDto containing only active users.
     */
    public Page<UserDto> getActiveUsers(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable).map(this::convertToDto);
    }

    /**
     * Gets all inactive users with pagination.
     * @param pageable The pagination parameters.
     * @return A Page of UserDto containing only inactive users.
     */
    public Page<UserDto> getInactiveUsers(Pageable pageable) {
        return userRepository.findByActiveFalse(pageable).map(this::convertToDto);
    }

    public UserStatsDto getUserStats() {
        UserStatsDto stats = new UserStatsDto();
        List<User> allUsers = userRepository.findAll();
        
        // Basic user counts
        stats.setTotalUsers(allUsers.size());
        stats.setActiveUsers(allUsers.stream().filter(User::getActive).count());
        stats.setInactiveUsers(allUsers.stream().filter(u -> !u.getActive()).count());
        stats.setSuspendedUsers(allUsers.stream().filter(u -> u.getRole().equalsIgnoreCase("SUSPENDED")).count());
        
        // Note: newUsersThisMonth calculation removed since User entity doesn't have createdAt field
        // You can add this field to User entity if needed, or calculate based on registration date
        stats.setNewUsersThisMonth(0); // Set to 0 for now
        
        // Calculate new users this month using createdAt field
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime firstDayOfMonthStart = firstDayOfMonth.atStartOfDay();
        long newUsersThisMonth = allUsers.stream()
            .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(firstDayOfMonthStart))
            .count();
        stats.setNewUsersThisMonth(newUsersThisMonth);
        
        try {
            // Calculate monthly revenue
            LocalDateTime endOfMonth = firstDayOfMonth.plusMonths(1).atStartOfDay();
            
            BigDecimal monthlyRevenue = orderRepository.calculateMonthlyRevenue(firstDayOfMonthStart, endOfMonth);
            stats.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);
            
            // Calculate total revenue
            BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
            stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Calculate order statistics
            long monthlyPendingOrders = orderRepository.countPendingOrdersForMonth(firstDayOfMonthStart, endOfMonth);
            long monthlyDeliveredOrders = orderRepository.countDeliveredOrdersForMonth(firstDayOfMonthStart, endOfMonth);
            long totalPendingOrders = orderRepository.countTotalPendingOrders();
            long totalDeliveredOrders = orderRepository.countTotalDeliveredOrders();
            
            stats.setMonthlyPendingOrders(monthlyPendingOrders);
            stats.setMonthlyDeliveredOrders(monthlyDeliveredOrders);
            stats.setTotalPendingOrders(totalPendingOrders);
            stats.setTotalDeliveredOrders(totalDeliveredOrders);
            
        } catch (Exception e) {
            // Log error and set revenue to zero if calculation fails
            System.err.println("Error calculating revenue and order stats: " + e.getMessage());
            stats.setMonthlyRevenue(BigDecimal.ZERO);
            stats.setTotalRevenue(BigDecimal.ZERO);
            stats.setMonthlyPendingOrders(0);
            stats.setMonthlyDeliveredOrders(0);
            stats.setTotalPendingOrders(0);
            stats.setTotalDeliveredOrders(0);
        }
        
        // Users by role
        HashMap<String, Long> usersByRole = new HashMap<>();
        allUsers.forEach(u -> {
            String role = u.getRole();
            usersByRole.put(role, usersByRole.getOrDefault(role, 0L) + 1);
        });
        stats.setUsersByRole(usersByRole);
        
        return stats;
    }
}