package com.udhr.controller;

import com.udhr.dto.PatientRequest;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.Patient;
import com.udhr.service.PatientService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

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

    @GetMapping("/{idNumber}")
    public ResponseEntity<?> getPatientByIdNumber(@PathVariable String idNumber, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        Patient patient = patientService.findByIdNumber(idNumber, staffNumber);
        return ResponseEntity.ok(patient);
    }

    @PostMapping
    public ResponseEntity<?> registerPatient(@RequestBody PatientRequest patientRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        Patient patient = patientService.registerPatient(patientRequest, staffNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    @GetMapping("/{idNumber}/record")
    public ResponseEntity<?> getFullPatientRecord(@PathVariable String idNumber, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        PatientRecordResponse record = patientService.getFullRecord(idNumber, staffNumber);
        return ResponseEntity.ok(record);
    }
}
