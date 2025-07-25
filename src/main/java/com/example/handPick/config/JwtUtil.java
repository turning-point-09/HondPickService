package com.example.handPick.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET; // Base64 encoded string

    @Value("${jwt.expiration}")
    private long expiration; // milliseconds for access token

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration; // milliseconds for refresh token

    /**
     * Decodes the Base64 secret string and creates a SecretKey for signing/verifying JWTs.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for an authenticated user.
     */
    public String generateAccessToken(String mobileNumber, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, mobileNumber, expiration);
    }

    /**
     * Generates a refresh token for an authenticated user.
     */
    public String generateRefreshToken(String mobileNumber) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, mobileNumber, refreshExpiration);
    }

    /**
     * Internal method to create a JWT.
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Extracts the mobile number (subject) from a given JWT.
     */
    public String extractMobileNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a given JWT.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim from the JWT.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a signed JWT.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates an access token against user details.
     */
    public Boolean validateToken(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        final String extractedMobileNumber = extractMobileNumber(token);
        return (extractedMobileNumber != null && extractedMobileNumber.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Checks if a JWT token has expired.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public long getExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}