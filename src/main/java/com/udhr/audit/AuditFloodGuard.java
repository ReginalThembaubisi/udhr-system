package com.udhr.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps how many "request without signing in" entries reach the audit trail.
 * Every such request is still refused; only the record of it is limited, so
 * someone sending thousands of requests can't bury real entries or fill the
 * database. Per address: the first few in each 10-minute window are written,
 * and one more says how many were left out. Overall there is a ceiling too,
 * for many addresses at once.
 */
@Component
public class AuditFloodGuard {

    static final int PER_ADDRESS = 5;
    static final int OVERALL = 200;
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, AtomicInteger> perAddress = new ConcurrentHashMap<>();
    private final AtomicInteger overall = new AtomicInteger();
    private volatile long windowStart = System.currentTimeMillis();

    /** What to do with one signed-out request: write it, note the cut-off once, or skip it. */
    public enum Decision { RECORD, RECORD_LIMIT_REACHED, SKIP }

    public Decision check(HttpServletRequest request) {
        rollWindow();
        int count = perAddress.computeIfAbsent(clientAddress(request), k -> new AtomicInteger()).incrementAndGet();
        int total = overall.incrementAndGet();
        if (count > PER_ADDRESS + 1 || total > OVERALL + 1) {
            return Decision.SKIP;
        }
        if (count == PER_ADDRESS + 1 || total == OVERALL + 1) {
            return Decision.RECORD_LIMIT_REACHED;
        }
        return Decision.RECORD;
    }

    private void rollWindow() {
        long now = System.currentTimeMillis();
        if (now - windowStart > WINDOW_MILLIS) {
            synchronized (this) {
                if (now - windowStart > WINDOW_MILLIS) {
                    perAddress.clear();
                    overall.set(0);
                    windowStart = now;
                }
            }
        }
    }

    // As AuthController does: Render's proxy puts the caller first in X-Forwarded-For.
    static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
