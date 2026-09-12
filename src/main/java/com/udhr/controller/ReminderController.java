package com.udhr.controller;

import com.udhr.model.MedicationAdherence;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.security.FacilityGuard;
import com.udhr.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getLoggedInStaffNumber() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/patient")
    public ResponseEntity<?> getPatientTodayReminders() {
        String idNumber = getLoggedInPatientId();
        List<MedicationAdherence> todayLogs = reminderService.getTodayAdherenceLogsForPatient(idNumber);

        // Get patient statistics based on patient ID from first log
        Map<String, Object> stats = new HashMap<>();
        if (!todayLogs.isEmpty()) {
            Long patientId = todayLogs.get(0).getPatient().getId();
            stats = reminderService.getAdherenceStats(patientId);
        } else {
            stats.put("totalDoses", 0);
            stats.put("takenDoses", 0);
            stats.put("missedDoses", 0);
            stats.put("pendingDoses", 0);
            stats.put("adherenceScore", 100);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("adherenceLogs", todayLogs);
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/adherence/{adherenceId}")
    public ResponseEntity<?> updateAdherenceStatus(
            @PathVariable Long adherenceId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String notes = request.getOrDefault("notes", "");
        if (status == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Status is required ('TAKEN' or 'MISSED')"));
        }

        MedicationAdherence updated = reminderService.updateAdherenceStatus(adherenceId, status, notes, getLoggedInPatientId());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/patient/{patientId}/adherence")
    public ResponseEntity<?> getPatientAdherenceLogsForDoctor(@PathVariable Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(getLoggedInStaffNumber())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
        FacilityGuard.assertSameFacility(staff, patient);

        List<MedicationAdherence> logs = reminderService.getWeeklyAdherenceLogsForPatient(patientId);
        Map<String, Object> stats = reminderService.getAdherenceStats(patientId);

        Map<String, Object> response = new HashMap<>();
        response.put("adherenceLogs", logs);
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }
}
