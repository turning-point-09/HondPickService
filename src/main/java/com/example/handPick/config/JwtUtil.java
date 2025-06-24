package com.example.handPick.config;

import io.jsonwebtoken.Claims; // Explicitly import Claims
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // Use javax.crypto.SecretKey
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // Import UUID
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET; // This will now be the Base64 encoded string

    @Value("${jwt.expiration}")
    private long expiration; // milliseconds for access token

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration; // milliseconds for refresh token

    @Value("${jwt.guest.expiration}")
    private long guestExpiration; // milliseconds for guest token

    /**
     * Decodes the Base64 secret string and creates a SecretKey for signing/verifying JWTs.
     * This method ensures the key is securely generated and compatible with HS512.
     * @return SecretKey suitable for HS512.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for an authenticated user.
     * @param username The username of the user.
     * @param userId The ID of the user.
     * @return A signed JWT access token.
     */
    public String generateAccessToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId); // Include userId in access token claims
        return createToken(claims, username, expiration);
    }

    /**
     * Generates a refresh token for an authenticated user.
     * Note: The actual refresh token value is stored in the database,
     * this method might not be directly used for generating the database stored value,
     * but could be used if the refresh token itself was a JWT (which it is not in our current model).
     * Keeping for consistency if a JWT refresh token were desired.
     * @param username The username of the user.
     * @return A signed JWT refresh token.
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        // No userId in refresh token claims as it's primarily for server-side lookup
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * Generates a token for guest users. The subject of this token will be the guestId.
     * @param guestId The UUID representing the guest.
     * @return A signed JWT guest token.
     */
    public String generateGuestToken(UUID guestId) { // Corrected parameter type to UUID
        Map<String, Object> claims = new HashMap<>();
        // No specific claims needed for guest, subject is the guestId
        return createToken(claims, guestId.toString(), guestExpiration); // Subject is UUID as String
    }

    /**
     * Internal method to create a JWT.
     * @param claims Custom claims to include in the JWT.
     * @param subject The subject of the JWT (e.g., username, guestId).
     * @param expirationTime The expiration time of the token in milliseconds.
     * @return The compact JWT string.
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .claims(claims) // Use claims()
                .subject(subject) // Use subject()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // Use expiration()
                .signWith(getSigningKey(), Jwts.SIG.HS512) // Correct way to specify HS512 with JJWT 0.12.x+
                .compact();
    }

    /**
     * Extracts the username (subject) from a given JWT.
     * @param token The JWT string.
     * @return The username (subject) string.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the guest ID (UUID) from a guest JWT.
     * Assumes the guest ID is stored as the 'subject' claim in the token.
     * @param token The guest JWT string.
     * @return The UUID of the guest, or null if the subject is not a valid UUID.
     */
    public UUID extractGuestId(String token) {
        String guestIdString = extractClaim(token, Claims::getSubject);
        try {
            return UUID.fromString(guestIdString);
        } catch (IllegalArgumentException e) {
            System.err.println("Token subject is not a valid UUID for guest ID: " + guestIdString);
            return null;
        }
    }

    /**
     * Extracts the expiration date from a given JWT.
     * @param token The JWT string.
     * @return The expiration Date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim from the JWT.
     * @param token The JWT string.
     * @param claimsResolver A function to resolve the desired claim from the Claims object.
     * @param <T> The type of the claim to be extracted.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a signed JWT.
     * @param token The JWT string.
     * @return The Claims object.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Use verifyWith() for parsing with SecretKey
                .build()
                .parseSignedClaims(token) // Use parseSignedClaims() for signed JWTs
                .getPayload(); // Get the Claims payload
    }

    /**
     * Validates an access token against user details.
     * @param token The JWT access token string.
     * @param userDetails The UserDetails object for comparison.
     * @return True if the token is valid for the user and not expired, false otherwise.
     */
    public Boolean validateToken(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername != null && extractedUsername.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Validates a guest token by checking its signature and expiration.
     * @param token The guest JWT string.
     * @return True if the guest token is valid and not expired, false otherwise.
     */
    public Boolean validateGuestToken(String token) {
        try {
            // Attempt to extract all claims; this will throw if signature is invalid or token is malformed
            extractAllClaims(token);
            // If extraction succeeds, just check expiration
            return !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("Guest token expired: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Guest token validation failed (malformed or invalid signature): " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a JWT token has expired.
     * @param token The JWT string.
     * @return True if the token is expired, false otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // --- Getters for expiration times (already present, ensuring consistency) ---

    public long getExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public long getGuestExpiration() {
        return guestExpiration;
    }
}