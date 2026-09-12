package com.udhr.controller;

import com.udhr.dto.LabResultRequest;
import com.udhr.model.LabResult;
import com.udhr.service.LabResultService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lab-results")
public class LabResultController {

    @Autowired
    private LabResultService labResultService;

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
    public ResponseEntity<?> addLabResult(@RequestBody LabResultRequest labResultRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        LabResult labResult = labResultService.addLabResult(labResultRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(labResult);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getLabResultsByPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        List<LabResult> labResults = labResultService.getLabResultsByPatient(patientId);
        return ResponseEntity.ok(labResults);
    }
}
