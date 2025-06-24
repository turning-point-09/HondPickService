package com.example.handPick.service; // UPDATED

import com.example.handPick.model.User; // UPDATED
import com.example.handPick.repository.UserRepository; // UPDATED
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists!");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // Encode the password
        newUser.setEmail(email);
        return userRepository.save(newUser);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}