package com.udhr.controller;

import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.service.DrugFoodAuditService;
import com.udhr.service.TreatmentResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinical-alerts")
public class ClinicalAlertController {

    @Autowired
    private ClinicalAlertRepository alertRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private SymptomCheckRepository symptomCheckRepository;

    @Autowired
    private LabRecommendationRepository labRecommendationRepository;

    @Autowired
    private DifferentialDiagnosisRepository differentialDiagnosisRepository;

    @Autowired
    private MedicationAdherenceRepository adherenceRepository;

    @Autowired
    private TreatmentResponseService treatmentResponseService;

    @Autowired
    private DrugFoodAuditService drugFoodAuditService;

    private String getLoggedInPatientId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<?> getUnresolvedAlerts() {
        try {
            List<ClinicalAlert> alerts = alertRepository.findByIsResolvedFalseOrderByCreatedAtDesc();
            List<Map<String, Object>> response = new ArrayList<>();

            for (ClinicalAlert alert : alerts) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("alert", alert);
                detail.put("patient", alert.getPatient());

                Patient patient = alert.getPatient();
                
                // Fetch active prescriptions
                List<Prescription> activeRx = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                        .stream().filter(Prescription::getActive).collect(Collectors.toList());
                detail.put("activePrescriptions", activeRx);

                // Calculate adherence score (last 14 days)
                LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
                List<MedicationAdherence> logs = adherenceRepository.findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                        patient.getId(), fourteenDaysAgo, LocalDateTime.now());
                long totalDoses = logs.stream().filter(l -> l.getScheduledTime().isBefore(LocalDateTime.now())).count();
                long takenDoses = logs.stream().filter(l -> "TAKEN".equals(l.getStatus())).count();
                int score = totalDoses > 0 ? (int) ((takenDoses * 100) / totalDoses) : 100;
                detail.put("adherenceScore", score);

                // Last 3 symptom checks
                List<SymptomCheck> checks = symptomCheckRepository.findByPatientIdOrderByCheckedAtDesc(patient.getId())
                        .stream().limit(3).collect(Collectors.toList());
                detail.put("recentSymptomChecks", checks);

                // Lab recommendations
                List<LabRecommendation> labRecs = labRecommendationRepository.findByAlertId(alert.getId());
                detail.put("labRecommendations", labRecs);

                // Differential diagnoses
                List<DifferentialDiagnosis> diffDiags = differentialDiagnosisRepository.findByAlertId(alert.getId());
                detail.put("differentialDiagnoses", diffDiags);

                response.add(detail);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        try {
            ClinicalAlert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Alert not found"));
            alert.setIsResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            ClinicalAlert saved = alertRepository.save(alert);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientTimelineData(@PathVariable Long patientId) {
        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

            Map<String, Object> data = new HashMap<>();
            data.put("patient", patient);
            
            // Adherence Logs in last 14 days
            List<MedicationAdherence> adherenceLogs = adherenceRepository.findByPatientIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                    patientId, fourteenDaysAgo, LocalDateTime.now().plusDays(1));
            data.put("adherenceLogs", adherenceLogs);

            // Symptom checks in last 14 days
            List<SymptomCheck> symptomChecks = symptomCheckRepository.findByPatientIdOrderByCheckedAtDesc(patientId)
                    .stream().filter(c -> c.getCheckedAt().isAfter(fourteenDaysAgo)).collect(Collectors.toList());
            data.put("symptomChecks", symptomChecks);

            // Active Prescriptions
            List<Prescription> rxList = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                    .stream().filter(Prescription::getActive).collect(Collectors.toList());
            data.put("prescriptions", rxList);

            // Active clinical alerts
            List<ClinicalAlert> activeAlerts = alertRepository.findByPatientIdAndIsResolvedFalse(patientId);
            data.put("activeAlerts", activeAlerts);

            // Food drug audit
            List<Map<String, Object>> conflicts = drugFoodAuditService.auditDrugFoodConflicts(patientId);
            data.put("foodConflicts", conflicts);

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/my-alerts")
    public ResponseEntity<?> getLoggedInPatientAlerts() {
        try {
            String idNumber = getLoggedInPatientId();
            Patient patient = patientRepository.findByIdNumber(idNumber)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            List<ClinicalAlert> activeAlerts = alertRepository.findByPatientIdAndIsResolvedFalse(patient.getId());
            return ResponseEntity.ok(activeAlerts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/drug-food-audit")
    public ResponseEntity<?> getPatientDrugFoodConflicts() {
        try {
            String idNumber = getLoggedInPatientId();
            Patient patient = patientRepository.findByIdNumber(idNumber)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            List<Map<String, Object>> conflicts = drugFoodAuditService.auditDrugFoodConflicts(patient.getId());
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/patient/{patientId}/evaluate")
    public ResponseEntity<?> manualEvaluatePatient(@PathVariable Long patientId) {
        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            treatmentResponseService.evaluatePatientResponse(patient);
            return ResponseEntity.ok(Map.of("message", "Evaluation completed successfully for patient ID: " + patientId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createManualAlert(@RequestBody Map<String, Object> request) {
        try {
            Long patientId = Long.valueOf(request.get("patientId").toString());
            String severityStr = request.get("severity").toString();
            String message = request.get("message").toString();

            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatient(patient);
            alert.setSeverity(ClinicalAlert.AlertSeverity.valueOf(severityStr.toUpperCase()));
            alert.setMessage("🧑‍⚕️ Custom Alert: " + message);
            alert.setAlertType(ClinicalAlert.AlertType.MANUAL);
            alert.setIsResolved(false);
            alert.setCreatedAt(LocalDateTime.now());

            ClinicalAlert saved = alertRepository.save(alert);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
