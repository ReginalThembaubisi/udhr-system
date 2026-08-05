package com.udhr.controller;

import com.udhr.model.Symptom;
import com.udhr.model.SymptomCheck;
import com.udhr.service.SymptomCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SymptomCheckerController {

    @Autowired
    private SymptomCheckerService symptomCheckerService;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/symptoms")
    public ResponseEntity<List<Symptom>> getSymptoms() {
        List<Symptom> symptoms = symptomCheckerService.getAllSymptoms();
        return ResponseEntity.ok(symptoms);
    }

    @PostMapping("/symptom-checker/check")
    public ResponseEntity<?> checkSymptoms(@RequestBody List<Long> symptomIds) {
        try {
            String idNumber = getLoggedInPatientId();
            SymptomCheck result = symptomCheckerService.performSymptomCheck(idNumber, symptomIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/symptom-checker/history")
    public ResponseEntity<?> getHistory() {
        try {
            String idNumber = getLoggedInPatientId();
            List<SymptomCheck> history = symptomCheckerService.getPatientHistory(idNumber);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
