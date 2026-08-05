package com.udhr.controller;

import com.udhr.dto.LoginRequest;
import com.udhr.dto.LoginResponse;
import com.udhr.dto.PatientLoginRequest;
import com.udhr.dto.PatientLoginResponse;
import com.udhr.service.AuthService;
import com.udhr.service.PatientPortalService;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/patient/login")
    public ResponseEntity<?> patientLogin(@RequestBody PatientLoginRequest request) {
        try {
            PatientLoginResponse response = patientPortalService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
