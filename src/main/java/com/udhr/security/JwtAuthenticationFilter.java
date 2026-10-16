package com.udhr.security;

import com.udhr.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Lazy because SecurityConfig needs this filter while building the security
    // chain, and TokenService pulls in repositories — without it the two sides
    // form a startup cycle.
    @Lazy
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, java.io.IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // A two-step sign-in challenge only proves the password was right;
            // it must never be accepted as a sign-in.
            if (jwtUtil.validateToken(token) && jwtUtil.extractPurpose(token) == null) {
                String staffNumber = jwtUtil.extractStaffNumber(token);
                String role = jwtUtil.extractRole(token);

                // A valid signature and an unexpired clock are no longer enough.
                // Two things can kill a token early, and both are checked here so
                // a stolen token stops working on its very next request:
                //
                //   1. the token itself was revoked (the user logged out), and
                //   2. every session for the account was revoked (password change,
                //      or an administrator forcing a compromised account out).
                String tokenId = jwtUtil.extractTokenId(token);
                if (tokenService.isAccessTokenRevoked(tokenId)) {
                    reject(response, "Session has ended. Please sign in again.");
                    return;
                }

                LocalDateTime issuedAt = jwtUtil.extractIssuedAt(token);
                if (tokenService.isIssuedBeforeMassRevocation(staffNumber, issuedAt)) {
                    reject(response, "Session has been ended for security reasons. Please sign in again.");
                    return;
                }

                // A token issued while the account still needs a password
                // change is only good for completing that change — every
                // other endpoint is blocked here, at the filter, so this
                // can't be bypassed by calling the API directly instead of
                // going through the frontend's gate screen.
                if (jwtUtil.extractMustChangePassword(token) && !"/api/auth/change-password".equals(request.getRequestURI())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("\"Password change required before continuing.\"");
                    return;
                }

                // Same idea when two-step sign-in is compulsory (STAFF_2FA=required)
                // and this account hasn't set it up yet: only the setup works.
                if (jwtUtil.extractTwoFactorSetup(token) && !request.getRequestURI().startsWith("/api/auth/2fa/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("\"Set up two-step sign-in before continuing.\"");
                    return;
                }

                if (staffNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            staffNumber,
                            null,
                            List.of(authority)
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("\"" + message + "\"");
    }
}
