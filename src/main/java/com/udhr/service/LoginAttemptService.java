package com.udhr.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts failed sign-ins and locks a key for 15 minutes once it has too many.
 *
 * Two kinds of key are counted for every staff sign-in:
 *
 *  - the staff number, so one account can't be guessed at forever (5 tries), and
 *  - the address the request came from, so one machine can't spray guesses
 *    across many accounts (20 tries — higher, because a whole clinic usually
 *    shares one public address and a few honest typos shouldn't lock everyone out).
 *
 * Counts are kept by the typed staff number, whether or not that account
 * exists, so an unknown number locks exactly like a real one and the lockout
 * message can't be used to discover which staff numbers are real. Held in
 * memory: the app runs as a single instance, and a restart clearing the
 * counters only gives a guesser a fresh 5 tries.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ACCOUNT_FAILURES = 5;
    public static final int MAX_ADDRESS_FAILURES = 20;
    public static final Duration LOCKOUT = Duration.ofMinutes(15);

    private record Window(int failures, Instant firstFailure, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public static String accountKey(String staffNumber) {
        return "staff:" + (staffNumber == null ? "" : staffNumber.trim().toUpperCase(Locale.ROOT));
    }

    public static String addressKey(String address) {
        return "address:" + (address == null ? "unknown" : address);
    }

    /** When the key's lock ends, or null if it isn't locked. */
    public Instant lockedUntil(String key) {
        Window window = windows.get(key);
        if (window == null || window.lockedUntil() == null) {
            return null;
        }
        if (window.lockedUntil().isBefore(Instant.now())) {
            windows.remove(key, window);
            return null;
        }
        return window.lockedUntil();
    }

    /**
     * Records one failure and returns true if it is the one that locked the key,
     * so the caller can write the lockout to the audit trail exactly once.
     */
    public boolean recordFailure(String key, int maxFailures) {
        Instant now = Instant.now();
        Window updated = windows.compute(key, (k, current) -> {
            // Failures older than the lockout period no longer count.
            if (current == null || current.firstFailure().plus(LOCKOUT).isBefore(now)) {
                current = new Window(0, now, null);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(LOCKOUT) : current.lockedUntil();
            return new Window(failures, current.firstFailure(), lockedUntil);
        });
        return updated.failures() == maxFailures;
    }

    /**
     * "in 12 minutes" for a lockout message. A clock time would be the
     * server's (UTC on Render), two hours off from a South African clinic.
     */
    public static String waitText(Instant lockedUntil) {
        long minutes = Math.max(1, (Duration.between(Instant.now(), lockedUntil).getSeconds() + 59) / 60);
        return "in " + minutes + (minutes == 1 ? " minute" : " minutes");
    }

    public void reset(String key) {
        windows.remove(key);
    }
}
