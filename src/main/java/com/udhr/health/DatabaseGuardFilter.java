package com.udhr.health;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * When the database can't be reached (stopped, moved, wrong address), every
 * API request gets one clear message instead of a stack trace or a half-saved
 * form. Nothing is attempted, so nothing is half-saved. The pages themselves
 * still load, and /api/system-status says what's wrong.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class DatabaseGuardFilter extends OncePerRequestFilter {

    public static final String MESSAGE = "The database can't be reached right now, so nothing was saved or loaded. "
            + "Try again in a minute. If it keeps happening, tell the system administrator.";

    private final DatabaseHealth health;

    public DatabaseGuardFilter(DatabaseHealth health) {
        this.health = health;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/system-status");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!health.isUp()) {
            writeUnavailable(response);
            return;
        }
        chain.doFilter(request, response);
    }

    static void writeUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "30");
        response.getWriter().write("{\"databaseUnavailable\":true,\"message\":\"" + MESSAGE + "\"}");
    }
}
