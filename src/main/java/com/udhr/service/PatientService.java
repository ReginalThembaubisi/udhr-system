package com.udhr.service;

import com.udhr.dto.PatientRequest;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private ChronicConditionRepository chronicConditionRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VitalsRepository vitalsRepository;

    /**
     * The one lookup every staff-facing service should use going forward:
     * patients may be found by national ID number, MRN, or passport number,
     * since not every patient has an ID number.
     */
    public Patient findByIdentifier(String identifier) {
        return patientRepository.findByIdNumber(identifier)
                .or(() -> patientRepository.findByMrn(identifier))
                .or(() -> patientRepository.findByPassportNumber(identifier))
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    /**
     * Generates a unique Medical Record Number in the form MRN-{year}-{6 digits},
     * retrying on the rare chance of a collision.
     */
    private String generateMrn() {
        String year = String.valueOf(Year.now().getValue());
        for (int attempt = 0; attempt < 10; attempt++) {
            int sequence = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            String candidate = String.format("MRN-%s-%06d", year, sequence);
            if (!patientRepository.existsByMrn(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Failed to generate a unique MRN, please try again");
    }

    public Patient registerPatient(PatientRequest request, String staffNumber) {
        if (request.getIdNumber() != null && !request.getIdNumber().isBlank()
                && patientRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("Patient already registered");
        }
        if (request.getPassportNumber() != null && !request.getPassportNumber().isBlank()
                && patientRepository.findByPassportNumber(request.getPassportNumber()).isPresent()) {
            throw new RuntimeException("Patient already registered");
        }

        Patient patient = new Patient();
        patient.setMrn(generateMrn());
        patient.setIdNumber(request.getIdNumber() != null && !request.getIdNumber().isBlank() ? request.getIdNumber() : null);
        patient.setPassportNumber(request.getPassportNumber() != null && !request.getPassportNumber().isBlank() ? request.getPassportNumber() : null);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());

        Patient savedPatient = patientRepository.save(patient);

        // Log the register action in AuditLog
        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            AuditLog auditLog = new AuditLog();
            auditLog.setStaff(staff);
            auditLog.setPatient(savedPatient);
            auditLog.setAction("REGISTER_PATIENT");
            auditLog.setDescription("Registered new patient: " + savedPatient.getMrn());
            auditLogRepository.save(auditLog);
        }

        return savedPatient;
    }

    public PatientRecordResponse getFullRecord(String identifier, String staffNumber) {
        Patient patient = findByIdentifier(identifier);

        List<Allergy> allergies = allergyRepository.findByPatientId(patient.getId());
        List<ChronicCondition> chronicConditions = chronicConditionRepository.findByPatientId(patient.getId());
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patient.getId());
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<LabResult> labResults = labResultRepository.findByPatientIdOrderByTestDateDesc(patient.getId());
        List<Vitals> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        Visit currentVisit = visits.isEmpty() ? null : visits.get(0);

        // Log the view action in AuditLog
        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            AuditLog auditLog = new AuditLog();
            auditLog.setStaff(staff);
            auditLog.setPatient(patient);
            auditLog.setAction("VIEW_RECORD");
            auditLog.setDescription("Viewed full record of patient: " + patient.getMrn());
            auditLogRepository.save(auditLog);
        }

        return new PatientRecordResponse(
                patient,
                allergies,
                chronicConditions,
                visits,
                diagnoses,
                prescriptions,
                labResults,
                vitals,
                currentVisit
        );
    }
}
