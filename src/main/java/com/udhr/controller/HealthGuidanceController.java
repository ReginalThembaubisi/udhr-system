package com.udhr.controller;

import com.udhr.dto.HealthGuidanceResponse;
import com.udhr.service.HealthGuidanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-guidance")
public class HealthGuidanceController {

    @Autowired
    private HealthGuidanceService healthGuidanceService;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/tips")
    public ResponseEntity<?> getHealthGuidance() {
        String idNumber = getLoggedInPatientId();
        HealthGuidanceResponse response = healthGuidanceService.getPersonalGuidance(idNumber);
        return ResponseEntity.ok(response);
    }
}
