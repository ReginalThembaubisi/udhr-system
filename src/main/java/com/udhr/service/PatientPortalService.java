package com.udhr.service;

import com.udhr.dto.PatientLoginRequest;
import com.udhr.dto.PatientLoginResponse;
import com.udhr.exception.LoginLockedException;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class PatientPortalService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

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
    private AuditService auditService;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    // One message for every wrong detail, so the login can't be used to find
    // out which ID numbers are registered or which part was wrong.
    private static final String INVALID = "ID number, date of birth or PIN is incorrect";

    public PatientLoginResponse login(PatientLoginRequest request, String clientAddress) {
        // Each patient account locks after 5 failures (below). This also stops
        // one machine guessing across many ID numbers, the same limit as staff.
        String addressKey = LoginAttemptService.addressKey(clientAddress);
        java.time.Instant addressLockedUntil = loginAttemptService.lockedUntil(addressKey);
        if (addressLockedUntil != null) {
            throw new LoginLockedException("Too many incorrect attempts. Try again "
                    + LoginAttemptService.waitText(addressLockedUntil) + ".");
        }

        Patient patient;
        try {
            patient = patientService.findByIdentifier(request.getIdNumber() == null ? "" : request.getIdNumber().trim());
        } catch (Exception e) {
            recordAddressFailure(addressKey, clientAddress);
            throw new RuntimeException(INVALID);
        }

        if (patient.getLoginLockedUntil() != null && patient.getLoginLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LoginLockedException("Too many incorrect attempts. Try again "
                    + LoginAttemptService.waitText(patient.getLoginLockedUntil().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    + ", or ask the front desk at your clinic to reset your PIN.");
        }

        if (patient.getPortalPinHash() == null) {
            throw new RuntimeException("Your portal PIN hasn't been set yet. Ask the front desk at your clinic to set one for you.");
        }

        LocalDate dob = null;
        try {
            dob = LocalDate.parse(request.getDateOfBirth());
        } catch (Exception ignored) {
            // treated as a wrong date of birth below
        }
        boolean correct = dob != null
                && dob.equals(patient.getDateOfBirth())
                && request.getPin() != null
                && passwordEncoder.matches(request.getPin().trim(), patient.getPortalPinHash());

        if (!correct) {
            int failures = (patient.getFailedLoginAttempts() == null ? 0 : patient.getFailedLoginAttempts()) + 1;
            patient.setFailedLoginAttempts(failures);
            if (failures >= MAX_FAILED_ATTEMPTS) {
                patient.setLoginLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                patient.setFailedLoginAttempts(0);
                auditService.recordView(null, patient, "PATIENT_LOGIN_LOCKED",
                        "Patient portal locked for " + LOCKOUT_MINUTES + " minutes after " + MAX_FAILED_ATTEMPTS + " failed sign-in attempts");
            }
            patientRepository.save(patient);
            recordAddressFailure(addressKey, clientAddress);
            throw new RuntimeException(INVALID);
        }

        patient.setFailedLoginAttempts(0);
        patient.setLoginLockedUntil(null);
        patientRepository.save(patient);

        return buildSession(patient);
    }

    private void recordAddressFailure(String addressKey, String clientAddress) {
        if (loginAttemptService.recordFailure(addressKey, LoginAttemptService.MAX_ADDRESS_FAILURES)) {
            auditService.recordSecurityEvent(null, "PATIENT_LOGIN_LOCKED", "Patient portal sign-in locked for 15 minutes for address "
                    + clientAddress + " after " + LoginAttemptService.MAX_ADDRESS_FAILURES + " failed attempts");
        }
    }

    /** A fresh access token and refresh token, after sign-in or a refresh. */
    public PatientLoginResponse buildSession(Patient patient) {
        String token = jwtUtil.generateToken(patient.getIdNumber(), "PATIENT");
        String fullName = patient.getFirstName() + " " + patient.getLastName();

        return new PatientLoginResponse(
                token,
                tokenService.issueRefreshToken(patient),
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
        List<Vitals> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        Visit currentVisit = visits.isEmpty() ? null : visits.get(0);

        // No staff actor: the patient is reading their own record, so the trail
        // attributes this to the patient rather than to a staff member.
        auditService.recordView(null, patient, "PATIENT_VIEW_RECORD",
                "Patient viewed their own full digital health record");

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
