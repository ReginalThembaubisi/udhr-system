package com.udhr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String staffNumber, String role) {
        return generateToken(staffNumber, role, false);
    }

    public String generateToken(String staffNumber, String role, boolean mustChangePassword) {
        return Jwts.builder()
                .setSubject(staffNumber)
                .claim("role", role)
                .claim("mustChangePassword", mustChangePassword)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractStaffNumber(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // Baked into the token at issue time so it can be enforced by the filter
    // without a DB lookup on every request. A token issued before this claim
    // existed (or one for a patient) has no such claim, which is safe: absent
    // means "not required".
    public boolean extractMustChangePassword(String token) {
        Boolean value = getClaims(token).get("mustChangePassword", Boolean.class);
        return value != null && value;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
