package com.example.handPick.service;

import com.example.handPick.model.User;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // For empty authorities list

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch user with address details if needed for other parts of the app,
        // but for authentication, basic user details (username, password) are sufficient.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // As per your instruction, we are NOT using roles for authorization.
        // Therefore, we provide an empty list of authorities.
        // The check for userId == 1 will be done explicitly in controllers/services.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>() // No authorities (roles) as per instruction
        );
    }
}
