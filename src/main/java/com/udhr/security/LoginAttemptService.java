package com.udhr.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal, in-memory brute-force guard for the login endpoints. Locks out
 * a given login key (e.g. "staff:DOC001" or "patient:9001015000083") after
 * repeated failures. This is intentionally simple -- a single-instance,
 * in-memory limiter -- but it closes off unlimited-speed password/DOB
 * guessing, which is the immediate risk on both login endpoints.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_SECONDS = 15 * 60;

    private static final class Attempts {
        int failureCount;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public void assertNotLocked(String key) {
        Attempts attempts = attemptsByKey.get(normalize(key));
        if (attempts != null && attempts.lockedUntil != null && Instant.now().isBefore(attempts.lockedUntil)) {
            throw new AccessDeniedException("Too many failed login attempts. Please try again in a few minutes.");
        }
    }

    public void recordFailure(String key) {
        Attempts attempts = attemptsByKey.computeIfAbsent(normalize(key), k -> new Attempts());
        synchronized (attempts) {
            attempts.failureCount++;
            if (attempts.failureCount >= MAX_ATTEMPTS) {
                attempts.lockedUntil = Instant.now().plusSeconds(LOCKOUT_SECONDS);
            }
        }
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
