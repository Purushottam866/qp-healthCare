package com.healthMini.helper;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;



@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;  // 3 days in milliseconds

    // Generate JWT Token with User ID, Email, and Role
    public String generateToken(int userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role); // Add role to claims

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // Extract Claims from Token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract User ID
    public int extractUserId(String token) {
        try {
            return extractAllClaims(token).get("userId", Integer.class);
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT Token expired!");
        }
    }

    // Extract Email
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    // Extract Role
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // Validate JWT Token
    public boolean validateToken(String token, String email) {
        return email.equals(extractEmail(token)) && !isTokenExpired(token);
    }

    // Check if Token is Expired
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}



