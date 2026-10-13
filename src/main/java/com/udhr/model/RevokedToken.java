package com.udhr.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * One row per access token that has been killed before its own expiry time —
 * the "stop trusting this token" list.
 *
 * A JWT is normally self-contained: the server checks the signature and the
 * expiry time and nothing else, which means a stolen token keeps working until
 * it expires on its own and logging out does nothing on the server. Every token
 * this system issues carries a unique id (the standard `jti` claim), and
 * JwtAuthenticationFilter rejects any token whose id appears here.
 *
 * This covers revoking ONE token (a logout). Killing every session a staff
 * member has at once — on a password change, or when a system administrator
 * forces them out — is handled by Staff.sessionsValidFrom instead, because
 * that doesn't require knowing which tokens are currently out there.
 *
 * Rows are kept until the token's natural expiry passes, after which the token
 * is refused on its expiry alone and the row is just clutter; RevokedTokenRepository
 * provides the cleanup query.
 */
@Data
@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_tokens_token_id", columnList = "token_id")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The token's `jti` claim.
    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    // Who the token belonged to — staff number, or the patient identifier for
    // a patient portal token. Kept for the audit trail, not used for matching.
    @Column(name = "subject")
    private String subject;

    // Why it was killed: LOGOUT, PASSWORD_CHANGE, ADMIN_FORCE_LOGOUT.
    @Column(name = "reason")
    private String reason;

    // The moment the token would have expired on its own. Once this has passed
    // the row can be deleted safely.
    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        this.revokedAt = LocalDateTime.now();
    }
}
