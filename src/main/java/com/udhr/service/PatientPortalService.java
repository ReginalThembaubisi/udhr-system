package com.udhr.service;

import com.udhr.dto.PatientLoginRequest;
import com.udhr.dto.PatientLoginResponse;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class PatientPortalService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtUtil jwtUtil;

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
    private ImmunizationRepository immunizationRepository;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private DispenseRepository dispenseRepository;

    public PatientLoginResponse login(PatientLoginRequest request) {
        Patient patient = patientRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("Patient not found with ID number: " + request.getIdNumber()));

        LocalDate dob;
        try {
            dob = LocalDate.parse(request.getDateOfBirth());
        } catch (Exception e) {
            throw new RuntimeException("Invalid date of birth format. Use YYYY-MM-DD");
        }

        if (!patient.getDateOfBirth().equals(dob)) {
            throw new RuntimeException("Invalid credentials (ID number or date of birth is incorrect)");
        }

        String token = jwtUtil.generateToken(patient.getIdNumber(), "PATIENT");
        String fullName = patient.getFirstName() + " " + patient.getLastName();

        return new PatientLoginResponse(
                token,
                patient.getIdNumber(),
                fullName,
                "PATIENT"
        );
    }

    public Patient getPatientProfile(String idNumber) {
        return patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public PatientRecordResponse getPatientRecord(String idNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        List<Allergy> allergies = allergyRepository.findByPatientId(patient.getId());
        List<ChronicCondition> chronicConditions = chronicConditionRepository.findByPatientId(patient.getId());
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patient.getId());
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<LabResult> labResults = labResultRepository.findByPatientIdOrderByTestDateDesc(patient.getId());
        List<Immunization> immunizations = immunizationRepository.findByPatientIdOrderByScheduledDateAsc(patient.getId());
        List<Vitals> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        List<Referral> referrals = referralRepository.findByPatientIdOrderByReferredAtDesc(patient.getId());
        List<Dispense> dispenses = dispenseRepository.findByPatientIdOrderByDispensedAtDesc(patient.getId());

        // Log the view action in AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setPatient(patient);
        auditLog.setAction("PATIENT_VIEW_RECORD");
        auditLog.setDescription("Patient viewed their own full digital health record");
        auditLogRepository.save(auditLog);

        return new PatientRecordResponse(
                patient,
                allergies,
                chronicConditions,
                visits,
                diagnoses,
                prescriptions,
                labResults,
                immunizations,
                vitals,
                referrals,
                dispenses
        );
    }
}
