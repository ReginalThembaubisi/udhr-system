package com.udhr.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;

    private static class Attempts {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStartedAt = System.currentTimeMillis();
        volatile long lockedUntil = 0;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempts a = attemptsByKey.get(key);
        return a != null && a.lockedUntil > System.currentTimeMillis();
    }

    public void recordFailure(String key) {
        Attempts a = attemptsByKey.computeIfAbsent(key, k -> new Attempts());
        long now = System.currentTimeMillis();
        if (now - a.windowStartedAt > WINDOW_MILLIS) {
            a.count.set(0);
            a.windowStartedAt = now;
        }
        if (a.count.incrementAndGet() >= MAX_ATTEMPTS) {
            a.lockedUntil = now + WINDOW_MILLIS;
        }
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }
}
