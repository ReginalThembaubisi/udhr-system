package com.udhr.controller;

import com.udhr.model.MedicationAdherence;
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

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/patient")
    public ResponseEntity<?> getPatientTodayReminders() {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/adherence/{adherenceId}")
    public ResponseEntity<?> updateAdherenceStatus(
            @PathVariable Long adherenceId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            String notes = request.getOrDefault("notes", "");
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Status is required ('TAKEN' or 'MISSED')");
            }

            MedicationAdherence updated = reminderService.updateAdherenceStatus(adherenceId, status, notes);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}/adherence")
    public ResponseEntity<?> getPatientAdherenceLogsForDoctor(@PathVariable Long patientId) {
        try {
            List<MedicationAdherence> logs = reminderService.getWeeklyAdherenceLogsForPatient(patientId);
            Map<String, Object> stats = reminderService.getAdherenceStats(patientId);

            Map<String, Object> response = new HashMap<>();
            response.put("adherenceLogs", logs);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
