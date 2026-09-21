package com.udhr.service;

import com.udhr.dto.PharmacyLookupResponse;
import com.udhr.model.AuditLog;
import com.udhr.model.Patient;
import com.udhr.model.Prescription;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.AuditLogRepository;
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
    private PatientService patientService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Pharmacist looks the patient up by ID number or MRN — no need to ask
     * which doctor sent them or chase a paper script. This shows exactly
     * where the patient came from and what's still waiting to be dispensed.
     */
    public PharmacyLookupResponse lookup(String identifier, String staffNumber) {
        Patient patient = patientService.findByIdentifier(identifier);

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
            auditLog.setDescription("Pharmacy looked up patient: " + patient.getMrn());
            auditLogRepository.save(auditLog);
        }

        return new PharmacyLookupResponse(patient, currentVisit, pending);
    }

    /**
     * The pharmacist's automatic dispense list: every visit at this facility
     * the doctor has finished with (status DIAGNOSED) that still has a
     * pending pharmacy-bound prescription attached — no ID lookup needed,
     * the doctor's hand-off is what puts a patient here.
     */
    public List<PharmacyLookupResponse> getQueue(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        List<Visit> diagnosedVisits = visitRepository
                .findByFacilityIdAndStatusOrderByVisitDateAsc(staff.getFacility().getId(), "DIAGNOSED");

        return diagnosedVisits.stream()
                .map(visit -> {
                    List<Prescription> pending = prescriptionRepository
                            .findByPatientIdAndDispenseMethodAndDispensedFalseOrderByCreatedAtDesc(
                                    visit.getPatient().getId(), "PHARMACY")
                            .stream()
                            .filter(p -> p.getVisit() != null && p.getVisit().getId().equals(visit.getId()))
                            .collect(java.util.stream.Collectors.toList());
                    return new PharmacyLookupResponse(visit.getPatient(), visit, pending);
                })
                .filter(response -> !response.getPendingPrescriptions().isEmpty())
                .collect(java.util.stream.Collectors.toList());
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
