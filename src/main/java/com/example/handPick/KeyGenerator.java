package com.example.handPick;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) {
        // Generate a secure key for HS512
        byte[] secretKeyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();

        // Base64 encode the key to store it in application.properties
        String base64Secret = Base64.getEncoder().encodeToString(secretKeyBytes);

        System.out.println("Generated HS512 Secret Key (Base64 encoded):");
        System.out.println(base64Secret);

        // Verify the key length in bits
        System.out.println("Key size in bits: " + (secretKeyBytes.length * 8));
    }
}