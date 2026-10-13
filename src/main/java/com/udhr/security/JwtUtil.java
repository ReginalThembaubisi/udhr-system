package com.udhr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

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
        // Every token gets a unique id (`jti`) so an individual token can be
        // named and revoked later — without it, a stolen token could only be
        // waited out, never killed.
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
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

    // The token's unique id, used to look it up on the revocation list.
    public String extractTokenId(String token) {
        return getClaims(token).getId();
    }

    // When the token was issued. Compared against the account's
    // sessionsValidFrom to reject tokens minted before a mass revocation.
    public LocalDateTime extractIssuedAt(String token) {
        Date issuedAt = getClaims(token).getIssuedAt();
        return issuedAt == null
                ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(issuedAt.getTime()), ZoneId.systemDefault());
    }

    // When the token would expire on its own — stored alongside a revocation
    // so the revocation row can be cleaned up once it stops mattering.
    public LocalDateTime extractExpiration(String token) {
        Date expiresAt = getClaims(token).getExpiration();
        return expiresAt == null
                ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAt.getTime()), ZoneId.systemDefault());
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
