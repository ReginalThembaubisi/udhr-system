package com.udhr.controller;

import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.dto.PatientLoginRequest;
import com.udhr.dto.PatientLoginResponse;
import com.udhr.security.LoginRateLimiter;
import com.udhr.service.AuthService;
import com.udhr.service.PatientPortalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PatientPortalService patientPortalService;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String key = clientIp(httpRequest) + ":" + request.getStaffNumber();
        if (loginRateLimiter.isBlocked(key)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many failed login attempts. Please try again in 15 minutes.");
        }

        try {
            LoginResponse response = authService.login(request);
            loginRateLimiter.recordSuccess(key);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loginRateLimiter.recordFailure(key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/patient/login")
    public ResponseEntity<?> patientLogin(@RequestBody PatientLoginRequest request, HttpServletRequest httpRequest) {
        String key = clientIp(httpRequest) + ":" + request.getIdNumber();
        if (loginRateLimiter.isBlocked(key)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many failed login attempts. Please try again in 15 minutes.");
        }

        try {
            PatientLoginResponse response = patientPortalService.login(request);
            loginRateLimiter.recordSuccess(key);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loginRateLimiter.recordFailure(key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
