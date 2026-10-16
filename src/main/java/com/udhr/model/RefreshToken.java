package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A long-lived credential that buys a new short-lived access token without
 * making the user log in again.
 *
 * Access tokens last 30 minutes so a stolen one is useful only briefly; without
 * something like this, staff would be typing their password back in every half
 * hour. The refresh token is an opaque random string, never a JWT, and only its
 * SHA-256 hash is stored — so this table leaking does not hand an attacker
 * working credentials, the same reasoning as hashing passwords.
 *
 * It is rotated on every use: refreshing consumes the current row and issues a
 * new one. If a stolen refresh token is used, the real user's next refresh fails
 * and the theft becomes visible rather than silently granting indefinite access.
 */
@Data
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SHA-256 of the token handed to the client. The raw value is shown once,
    // at issue, and never stored.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Exactly one of staff or patient is set: patients get refresh tokens too,
    // so a page reload doesn't sign them out now that the access token lives
    // only in the page's memory.
    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Set when the token is used (rotated), on logout, or when a system
    // administrator forces the account out. A revoked row is kept rather than
    // deleted so a replay of the old value is recognisable.
    @Column(name = "revoked", nullable = false)
    private Boolean revoked = false;

    // When the token was used up by a normal refresh. Two tabs refreshing at
    // the same moment both send the same token; the loser of that race is told
    // to retry instead of being treated as a thief (see TokenService).
    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Transient
    public boolean isUsable() {
        return !Boolean.TRUE.equals(revoked) && expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
