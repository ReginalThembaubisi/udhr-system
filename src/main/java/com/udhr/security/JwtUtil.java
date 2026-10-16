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

    public static final String TWO_FACTOR_CHALLENGE = "2fa-challenge";

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
        return generateToken(staffNumber, role, mustChangePassword, false);
    }

    /**
     * twoFactorSetup: the account must set up two-step sign-in before it can do
     * anything else (STAFF_2FA=required). Enforced by JwtAuthenticationFilter,
     * like mustChangePassword.
     */
    public String generateToken(String staffNumber, String role, boolean mustChangePassword, boolean twoFactorSetup) {
        // Every token gets a unique id (`jti`) so an individual token can be
        // named and revoked later — without it, a stolen token could only be
        // waited out, never killed.
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(staffNumber)
                .claim("role", role)
                .claim("mustChangePassword", mustChangePassword)
                .claim("twoFactorSetup", twoFactorSetup)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Proof that the password was right, good for 5 minutes and only for
     * entering the authenticator code. The filter never accepts it as a
     * sign-in (see extractPurpose).
     */
    public String generateTwoFactorChallenge(String staffNumber) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(staffNumber)
                .claim("purpose", TWO_FACTOR_CHALLENGE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Set only on special-purpose tokens, which must never count as a sign-in. */
    public String extractPurpose(String token) {
        return getClaims(token).get("purpose", String.class);
    }

    public boolean extractTwoFactorSetup(String token) {
        Boolean value = getClaims(token).get("twoFactorSetup", Boolean.class);
        return value != null && value;
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
