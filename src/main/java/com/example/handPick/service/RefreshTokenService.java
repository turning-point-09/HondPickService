package com.example.handPick.service; // UPDATED

import com.example.handPick.config.JwtUtil; // UPDATED
import com.example.handPick.model.RefreshToken; // UPDATED
import com.example.handPick.model.User; // UPDATED
import com.example.handPick.repository.RefreshTokenRepository; // UPDATED
import com.example.handPick.repository.UserRepository; // UPDATED
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // Although not directly used for generating refresh token content, good to have reference

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for refresh token creation."));

        // Check if user already has a refresh token, if so, update it or replace it
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            RefreshToken token = existingToken.get();
            token.setToken(UUID.randomUUID().toString()); // Generate a new UUID for the refresh token
            token.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            return refreshTokenRepository.save(token);
        } else {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(UUID.randomUUID().toString()); // Generate a random UUID as the refresh token value
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            return refreshTokenRepository.save(refreshToken);
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request.");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for refresh token deletion."));
        refreshTokenRepository.deleteByUser(user);
    }
}