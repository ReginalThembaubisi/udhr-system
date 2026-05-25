package com.udhr.controller;

import com.udhr.dto.AllergyRequest;
import com.udhr.model.Allergy;
import com.udhr.service.AllergyService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/allergies")
public class AllergyController {

    @Autowired
    private AllergyService allergyService;

    @Autowired
    private JwtUtil jwtUtil;

    private String extractStaffNumber(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.extractStaffNumber(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> addAllergy(@RequestBody AllergyRequest allergyRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Allergy allergy = allergyService.addAllergy(allergyRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(allergy);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getAllergiesByPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Allergy> allergies = allergyService.getAllergiesByPatient(patientId);
            return ResponseEntity.ok(allergies);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
