package com.udhr.controller;

import com.udhr.dto.ChangePasswordRequest;
import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.dto.PatientLoginRequest;
import com.udhr.dto.SessionResponse;
import com.udhr.exception.LoginLockedException;
import com.udhr.security.JwtUtil;
import com.udhr.service.AuthService;
import com.udhr.service.PatientPortalService;
import com.udhr.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * The refresh token lives only in this cookie (fix list P1-3):
     *  - HttpOnly, so no script on the page can read it, even an injected one;
     *  - Secure, so it never travels over plain HTTP (browsers allow it on localhost);
     *  - SameSite=Strict, so another site can't make the browser send it;
     *  - Path=/api/auth, so it is sent only to sign-in, refresh and sign-out;
     *  - no Max-Age, so it is gone when the browser closes. The server also
     *    expires it after a shift (jwt.refresh-expiration).
     */
    static final String REFRESH_COOKIE = "udhr_refresh";

    @Autowired
    private AuthService authService;

    @Autowired
    private PatientPortalService patientPortalService;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractStaffNumber(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        try {
            return jwtUtil.extractStaffNumber(token);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * The address used to limit failed sign-ins per machine. Render's proxy
     * puts the caller's address first in X-Forwarded-For. A caller can forge
     * that header to dodge the per-address limit, but the per-account lockout
     * still holds, so forging it buys no extra guesses at any one account.
     */
    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static ResponseCookie refreshCookie(String value) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(REFRESH_COOKIE, value == null ? "" : value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth");
        if (value == null) {
            cookie.maxAge(0); // tells the browser to delete it
        }
        return cookie.build();
    }

    private static ResponseEntity<?> withSession(SessionResponse session) {
        // No refresh token yet means sign-in isn't finished (two-step code
        // still needed), so no cookie is set.
        if (session.getRefreshToken() == null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.getRefreshToken()).toString())
                .body(session);
    }

    private static ResponseEntity<?> signIn(Supplier<SessionResponse> login) {
        try {
            return withSession(login.get());
        } catch (LoginLockedException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return signIn(() -> authService.login(request, clientAddress(httpRequest)));
    }

    /** Second step of a staff sign-in: the code from the authenticator app. */
    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return signIn(() -> authService.verifyTwoFactor(body.get("challengeToken"), body.get("code"), clientAddress(httpRequest)));
    }

    // ---- Two-step sign-in setup, for the signed-in staff member ------------------

    @GetMapping("/2fa/status")
    public ResponseEntity<?> twoFactorStatus(HttpServletRequest httpRequest) {
        return staffAction(httpRequest, staffNumber -> ResponseEntity.ok(twoFactorService.status(staffNumber)));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> twoFactorSetup(HttpServletRequest httpRequest) {
        return staffAction(httpRequest, staffNumber -> ResponseEntity.ok(twoFactorService.begin(staffNumber)));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<?> twoFactorConfirm(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return staffAction(httpRequest, staffNumber -> withSession(twoFactorService.confirm(staffNumber, body.get("code"), extractToken(httpRequest))));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<?> twoFactorDisable(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return staffAction(httpRequest, staffNumber -> {
            twoFactorService.disable(staffNumber, body.get("password"), body.get("code"));
            return ResponseEntity.ok(Map.of("message", "Two-step sign-in turned off"));
        });
    }

    private ResponseEntity<?> staffAction(HttpServletRequest httpRequest, java.util.function.Function<String, ResponseEntity<?>> action) {
        String staffNumber = extractStaffNumber(httpRequest);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }
        try {
            return action.apply(staffNumber);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/patient/login")
    public ResponseEntity<?> patientLogin(@RequestBody PatientLoginRequest request, HttpServletRequest httpRequest) {
        return signIn(() -> patientPortalService.login(request, clientAddress(httpRequest)));
    }

    /**
     * Trades the refresh cookie for a new short-lived access token, for staff
     * and patients alike. Open to unauthenticated callers by design: the expired
     * access token can't authorise anything, so the cookie itself is the
     * credential. The page calls this when it loads (the access token is kept
     * only in memory) and whenever an API call comes back 401.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        try {
            return withSession(authService.refresh(refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Ends the session on the server, not just in the browser: the access token
     * is added to the revoked list, the refresh token is revoked and its cookie
     * deleted. `reason=idle` marks an automatic sign-out after inactivity.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                    @RequestParam(name = "reason", required = false) String reason,
                                    HttpServletRequest httpRequest) {
        authService.logout(extractToken(httpRequest), refreshToken, extractStaffNumber(httpRequest),
                "idle".equals(reason) ? "IDLE_TIMEOUT" : "LOGOUT");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(null).toString())
                .body(Map.of("message", "Signed out"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String staffNumber = extractStaffNumber(httpRequest);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            LoginResponse response = authService.changePassword(staffNumber, request);
            return withSession(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
