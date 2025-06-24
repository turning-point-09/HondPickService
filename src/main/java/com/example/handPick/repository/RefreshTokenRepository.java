package com.example.handPick.repository; // UPDATED

import com.example.handPick.model.RefreshToken; // UPDATED
import com.example.handPick.model.User; // UPDATED
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    void deleteByUser(User user);
}