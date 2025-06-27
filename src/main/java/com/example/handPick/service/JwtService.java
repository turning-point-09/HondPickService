package com.example.handPick.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // CONSIDERATION: For production, this key must be much longer (at least 32 bytes for HS256),
    // and securely stored (e.g., in environment variables).
    // This is a base64 encoded 32-byte key (256 bits).
//    private static final String SECRET_KEY = "YourSuperSecretKeyThatIsAtLeast32BytesLongAndSecurelyStored!1234567890"; // Replaced with a sample key

    private static final String SECRET_KEY = "dS1DlPtWbEN8nLmDDKChi0o+kH44cIHd2PBX8Tdix7+egiSYyjyRzLQkmYxi07xZ2q2b02d4U8DRvFdNQolvUg==";
    // Token expiration time (e.g., 30 minutes)
    private static final long EXPIRATION_MILLIS = 1000 * 60 * 30 * 24;

    // Generates a token with just a subject (username). No extra claims.
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MILLIS);
        return Jwts.builder()
                .setSubject(username) // The only claim explicitly set here
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Retrieves the signing key from the base64 encoded secret.
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Extracts the subject (username) from the token.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts the expiration date from the token.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic method to extract any claim from the token.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parses and validates the token, returning all claims.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validates the token. Checks for expiration and if username matches (optional for "plain" tokens).
    // For a truly "plain" token without specific user details in the app, you might remove userDetails check.
    public boolean isTokenValid(String token) { // Simplified: removed UserDetails parameter
        final String username = extractUsername(token); // Still extracts username (subject)
        return !extractExpiration(token).before(new Date()); // Only check expiration
    }
}