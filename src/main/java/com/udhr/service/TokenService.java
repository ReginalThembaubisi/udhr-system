package com.udhr.service;

import com.udhr.model.RefreshToken;
import com.udhr.model.RevokedToken;
import com.udhr.model.Staff;
import com.udhr.repository.RefreshTokenRepository;
import com.udhr.repository.RevokedTokenRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Everything to do with killing and renewing sessions.
 *
 * Three separate questions are answered here, and they need different tools:
 *
 *  - "Stop trusting THIS token" (a logout)  -> the revoked-token list, by `jti`.
 *  - "Stop trusting ALL of this account's tokens" (password change, or an
 *    administrator forcing a compromised account out) -> Staff.sessionsValidFrom,
 *    which needs no knowledge of which tokens exist.
 *  - "Give me a new access token without a fresh login" -> rotating refresh tokens.
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // ---- Access token revocation -------------------------------------------------

    /** True if this specific token has been killed before its own expiry. */
    public boolean isAccessTokenRevoked(String tokenId) {
        return tokenId != null && revokedTokenRepository.existsByTokenId(tokenId);
    }

    /**
     * Kills one access token. Used on logout, so signing out actually stops the
     * token working rather than only dropping it from the browser.
     */
    @Transactional
    public void revokeAccessToken(String token, String reason) {
        String tokenId;
        LocalDateTime expiresAt;
        String subject;
        try {
            tokenId = jwtUtil.extractTokenId(token);
            expiresAt = jwtUtil.extractExpiration(token);
            subject = jwtUtil.extractStaffNumber(token);
        } catch (Exception e) {
            // An unreadable token can't be used anyway — nothing to revoke.
            return;
        }
        if (tokenId == null || revokedTokenRepository.existsByTokenId(tokenId)) {
            return;
        }

        RevokedToken revoked = new RevokedToken();
        revoked.setTokenId(tokenId);
        revoked.setSubject(subject);
        revoked.setReason(reason);
        revoked.setTokenExpiresAt(expiresAt != null ? expiresAt : LocalDateTime.now().plusDays(1));
        revokedTokenRepository.save(revoked);
    }

    /**
     * Kills every session the account currently has — every access token already
     * issued stops working on its next request, and every refresh token is
     * revoked so no device can mint a new one.
     *
     * This is the answer to "a doctor's token was stolen and hasn't expired":
     * the administrator calls this and the stolen token is dead immediately.
     */
    @Transactional
    public void revokeAllSessions(Staff staff, String reason) {
        // Whole seconds, because a JWT's issued-at time is whole seconds: with
        // the fraction kept, the fresh token handed back after a password
        // change (issued in the same second) counted as "before" the cut-off
        // and was rejected immediately.
        staff.setSessionsValidFrom(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        staffRepository.save(staff);
        refreshTokenRepository.revokeAllForStaff(staff.getId());
    }

    /**
     * True if the token was issued before this account's sessions were last
     * mass-revoked. Checked on every request by JwtAuthenticationFilter.
     */
    public boolean isIssuedBeforeMassRevocation(String staffNumber, LocalDateTime issuedAt) {
        if (issuedAt == null) {
            return false;
        }
        return staffRepository.findByStaffNumber(staffNumber)
                .map(Staff::getSessionsValidFrom)
                .map(validFrom -> issuedAt.isBefore(validFrom.truncatedTo(ChronoUnit.SECONDS)))
                .orElse(false);
    }

    // ---- Refresh tokens ----------------------------------------------------------

    /**
     * Issues a new refresh token and returns the raw value — the only time it
     * exists in readable form. Only its hash is stored.
     */
    @Transactional
    public String issueRefreshToken(Staff staff) {
        byte[] raw = new byte[48];
        RANDOM.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setStaff(staff);
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Consumes a refresh token and returns the staff member it belonged to, or
     * throws if it is unknown, expired or already used.
     *
     * The row is revoked on use (rotation): a refresh token works exactly once.
     * If someone steals one and uses it, the real user's next refresh fails and
     * they are pushed back to the login screen — the theft surfaces instead of
     * granting quiet, indefinite access.
     */
    @Transactional
    public Staff consumeRefreshToken(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Session expired — please sign in again"));

        if (!stored.isUsable()) {
            // A token presented after it was already used or revoked is a strong
            // signal of theft or replay: drop every session this account has
            // rather than just refusing this one request.
            revokeAllSessions(stored.getStaff(), "REFRESH_TOKEN_REUSE");
            throw new RuntimeException("Session is no longer valid — please sign in again");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return stored.getStaff();
    }

    @Transactional
    public void revokeRefreshTokensFor(Staff staff) {
        refreshTokenRepository.revokeAllForStaff(staff.getId());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash token", e);
        }
    }

    // ---- Housekeeping ------------------------------------------------------------

    /**
     * Rows for tokens that have expired on their own no longer do any work — the
     * token is refused on its expiry alone. Cleared nightly so these tables don't
     * grow forever.
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpiredTokenRecords() {
        LocalDateTime now = LocalDateTime.now();
        int revokedRemoved = revokedTokenRepository.deleteExpiredBefore(now);
        int refreshRemoved = refreshTokenRepository.deleteExpiredBefore(now);
        if (revokedRemoved > 0 || refreshRemoved > 0) {
            System.out.println("[TokenService] Purged " + revokedRemoved + " expired revocation rows and "
                    + refreshRemoved + " expired refresh tokens");
        }
    }
}
