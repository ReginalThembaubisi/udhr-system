package com.udhr.controller;

import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.Patient;
import com.udhr.service.PatientPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient/me")
public class PatientPortalController {

    @Autowired
    private PatientPortalService patientPortalService;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<?> getProfile() {
        String idNumber = getLoggedInPatientId();
        Patient patient = patientPortalService.getPatientProfile(idNumber);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/record")
    public ResponseEntity<?> getRecord() {
        String idNumber = getLoggedInPatientId();
        PatientRecordResponse record = patientPortalService.getPatientRecord(idNumber);
        return ResponseEntity.ok(record);
    }
}
