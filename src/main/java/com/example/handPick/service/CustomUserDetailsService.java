package com.example.handPick.service;

import com.example.handPick.model.User;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String mobileNumber) throws UsernameNotFoundException {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + mobileNumber));

        // Spring Security requires roles to be prefixed with 'ROLE_' for hasRole('ADMIN') to work
        return new org.springframework.security.core.userdetails.User(
                user.getMobileNumber(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}