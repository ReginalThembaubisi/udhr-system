package com.udhr.controller;

import com.udhr.dto.VisitRequest;
import com.udhr.model.Visit;
import com.udhr.service.VisitService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    @Autowired
    private VisitService visitService;

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
    public ResponseEntity<?> addVisit(@RequestBody VisitRequest visitRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            Visit visit = visitService.addVisit(visitRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(visit);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getVisitsByPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        try {
            List<Visit> visits = visitService.getVisitsByPatient(patientId);
            return ResponseEntity.ok(visits);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
