package com.udhr.controller;

import com.udhr.dto.ChronicConditionRequest;
import com.udhr.model.ChronicCondition;
import com.udhr.service.ChronicConditionService;
import com.udhr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chronic-conditions")
public class ChronicConditionController {

    @Autowired
    private ChronicConditionService chronicConditionService;

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
    public ResponseEntity<?> addChronicCondition(@RequestBody ChronicConditionRequest chronicConditionRequest, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        ChronicCondition chronicCondition = chronicConditionService.addChronicCondition(chronicConditionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(chronicCondition);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getChronicConditionsByPatient(@PathVariable Long patientId, HttpServletRequest request) {
        String staffNumber = extractStaffNumber(request);
        if (staffNumber == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        List<ChronicCondition> chronicConditions = chronicConditionService.getChronicConditionsByPatient(patientId);
        return ResponseEntity.ok(chronicConditions);
    }
}
