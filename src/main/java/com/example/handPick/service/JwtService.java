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

    private static final String SECRET_KEY = "dS1DlPtWbEN8nLmDDKChi0o+kH44cIHd2PBX8Tdix7+egiSYyjyRzLQkmYxi07xZ2q2b02d4U8DRvFdNQolvUg==";
    private static final long EXPIRATION_MILLIS = 1000 * 60 * 60 * 12;

    public String generateToken(String mobileNumber) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MILLIS);
        return Jwts.builder()
                .setSubject(mobileNumber)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractMobileNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        return !extractExpiration(token).before(new Date());
    }
}