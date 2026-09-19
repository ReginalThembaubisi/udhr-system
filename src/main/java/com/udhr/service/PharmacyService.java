package com.udhr.service;

import com.udhr.dto.PharmacyLookupResponse;
import com.udhr.model.AuditLog;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.PrescriptionRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PharmacyService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Pharmacist looks the patient up by ID number — no need to ask which
     * doctor sent them or chase a paper script. This shows exactly where
     * the patient came from and what's still waiting to be dispensed.
     */
    public PharmacyLookupResponse lookup(String idNumber, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        Visit currentVisit = visits.isEmpty() ? null : visits.get(0);

        List<Prescription> pending = prescriptionRepository
                .findByPatientIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(patient.getId(), "PHARMACY");

        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            AuditLog auditLog = new AuditLog();
            auditLog.setStaff(staff);
            auditLog.setPatient(patient);
            auditLog.setAction("PHARMACY_LOOKUP");
            auditLog.setDescription("Pharmacy looked up patient: " + idNumber);
            auditLogRepository.save(auditLog);
        }

        return new PharmacyLookupResponse(patient, currentVisit, pending);
    }

    public Prescription dispense(Long prescriptionId, String staffNumber) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));

        if (Boolean.TRUE.equals(prescription.getDispensed())) {
            throw new RuntimeException("Prescription already dispensed");
        }

        Staff pharmacist = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        prescription.setDispensed(true);
        prescription.setDispensedAt(LocalDateTime.now());
        prescription.setDispensedBy(pharmacist);
        Prescription saved = prescriptionRepository.save(prescription);

        // If every pharmacy-bound prescription for this visit is now dispensed, close the visit out.
        Visit visit = saved.getVisit();
        if (visit != null) {
            List<Prescription> stillPending = prescriptionRepository
                    .findByPatientIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(saved.getPatient().getId(), "PHARMACY");
            boolean anyForThisVisit = stillPending.stream()
                    .anyMatch(p -> p.getVisit() != null && p.getVisit().getId().equals(visit.getId()));
            if (!anyForThisVisit) {
                visit.setStatus("COMPLETE");
                visitRepository.save(visit);
            }
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setStaff(pharmacist);
        auditLog.setPatient(saved.getPatient());
        auditLog.setAction("DISPENSE_PRESCRIPTION");
        auditLog.setDescription("Dispensed " + saved.getMedication() + " (prescription #" + saved.getId() + ")");
        auditLogRepository.save(auditLog);

        return saved;
    }
}
