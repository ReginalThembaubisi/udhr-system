package com.udhr.service;

import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TreatmentResponseService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicationAdherenceRepository adherenceRepository;

    @Autowired
    private SymptomCheckRepository symptomCheckRepository;

    @Autowired
    private ClinicalAlertRepository alertRepository;

    @Autowired
    private ClinicalDecisionSupportService cdsService;

    @Autowired
    private DrugFoodAuditService drugFoodAuditService;

    public void evaluateAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        for (Patient p : patients) {
            try {
                evaluatePatientResponse(p);
            } catch (Exception e) {
                System.err.println("Error evaluating patient response for ID " + p.getId() + ": " + e.getMessage());
            }
        }
    }

    public void evaluatePatientResponse(Patient patient) {
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        // 1. Calculate Adherence Score over the last 14 days
        List<MedicationAdherence> logs = adherenceRepository.findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                patient.getId(), fourteenDaysAgo, LocalDateTime.now());

        long totalDoses = logs.stream()
                .filter(log -> log.getScheduledTime().isBefore(LocalDateTime.now()))
                .count();

        long takenDoses = logs.stream()
                .filter(log -> "TAKEN".equals(log.getStatus()))
                .count();

        int adherenceScore = totalDoses > 0 ? (int) ((takenDoses * 100) / totalDoses) : 0;

        // 2. Fetch last symptom check in 14 days
        List<SymptomCheck> symptomChecks = symptomCheckRepository.findByPatientIdOrderByCheckedAtDesc(patient.getId());
        List<SymptomCheck> recentChecks = symptomChecks.stream()
                .filter(c -> c.getCheckedAt().isAfter(fourteenDaysAgo))
                .collect(Collectors.toList());

        if (recentChecks.isEmpty() || totalDoses == 0) {
            return; // Not enough data to diagnose non-response
        }

        SymptomCheck latestCheck = recentChecks.get(0);
        String urgency = latestCheck.getUrgencyLevel().toUpperCase();

        // 3. Evaluate non-response condition: Adherence >= 90% and urgency is RED or YELLOW
        if (adherenceScore >= 90 && ("RED".equals(urgency) || "YELLOW".equals(urgency))) {
            // Check if alert already exists
            Optional<ClinicalAlert> existingAlert = alertRepository
                    .findByPatientIdAndAlertTypeAndIsResolvedFalse(patient.getId(), ClinicalAlert.AlertType.NON_RESPONSE);

            if (existingAlert.isEmpty()) {
                ClinicalAlert alert = new ClinicalAlert();
                alert.setPatient(patient);
                alert.setAlertType(ClinicalAlert.AlertType.NON_RESPONSE);
                alert.setSeverity("RED".equals(urgency) ? ClinicalAlert.AlertSeverity.CRITICAL : ClinicalAlert.AlertSeverity.HIGH);
                
                String message = String.format("🚨 Treatment Non-Response: Patient '%s %s' has high medication compliance (%d%%) over the last 14 days, but symptom checker reports persistent urgency level '%s'. Last symptom checker notes: %s",
                        patient.getFirstName(), patient.getLastName(), adherenceScore, urgency, latestCheck.getRecommendation());
                alert.setMessage(message);
                
                ClinicalAlert savedAlert = alertRepository.save(alert);
                // Trigger clinical recommendations
                cdsService.generateRecommendationsForAlert(savedAlert);
            }
        }

        // 4. Evaluate Drug-Food Conflicts and fire alert if any critical/warning conflicts exist
        List<Map<String, Object>> conflicts = drugFoodAuditService.auditDrugFoodConflicts(patient.getId());
        if (!conflicts.isEmpty()) {
            Optional<ClinicalAlert> existingFoodAlert = alertRepository
                    .findByPatientIdAndAlertTypeAndIsResolvedFalse(patient.getId(), ClinicalAlert.AlertType.DRUG_FOOD);

            if (existingFoodAlert.isEmpty()) {
                ClinicalAlert alert = new ClinicalAlert();
                alert.setPatient(patient);
                alert.setAlertType(ClinicalAlert.AlertType.DRUG_FOOD);
                
                boolean hasCritical = conflicts.stream().anyMatch(c -> "CRITICAL".equals(c.get("severity")));
                alert.setSeverity(hasCritical ? ClinicalAlert.AlertSeverity.CRITICAL : ClinicalAlert.AlertSeverity.MEDIUM);

                String conflictList = conflicts.stream()
                        .map(c -> String.format("- %s (%s)", c.get("medication"), c.get("ingredient")))
                        .collect(Collectors.joining("\n"));

                alert.setMessage(String.format("⚠️ Drug-Food Conflict Detected: Patient scanned ingredients that interact with active medications:\n%s", conflictList));
                alertRepository.save(alert);
            }
        }
    }
}
