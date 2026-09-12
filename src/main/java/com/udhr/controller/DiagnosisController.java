package com.udhr.controller;

import com.udhr.dto.DiagnosisRequest;
import com.udhr.model.Diagnosis;
import com.udhr.service.DiagnosisService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/diagnoses")
public class DiagnosisController {

    @Autowired
    private DiagnosisService diagnosisService;

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
    public ResponseEntity<?> addDiagnosis(@RequestBody DiagnosisRequest diagnosisRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        Diagnosis diagnosis = diagnosisService.addDiagnosis(diagnosisRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosis);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getDiagnosesByPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByPatient(patientId);
        return ResponseEntity.ok(diagnoses);
    }
}
