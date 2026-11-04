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
            auditService.recordSecurityEvent(null, "PATIENT_LOGIN_FAILED", "attempted an unsuccessful patient portal sign-in");
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
            auditService.recordEvent(null, patient, "PATIENT_LOGIN_FAILED",
                    "attempted an unsuccessful patient portal sign-in", "PATIENT", patient.getId());
            recordAddressFailure(addressKey, clientAddress);
            throw new RuntimeException(INVALID);
        }

        patient.setFailedLoginAttempts(0);
        patient.setLoginLockedUntil(null);
        patientRepository.save(patient);
        auditService.recordEvent(null, patient, "PATIENT_LOGIN_SUCCESS",
                "signed in to their patient portal", "PATIENT", patient.getId());
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
        boolean requiresPinChange = Boolean.TRUE.equals(patient.getMustChangePortalPin());
        String token = jwtUtil.generatePatientToken(patient.getIdNumber(), requiresPinChange);
        String fullName = patient.getFirstName() + " " + patient.getLastName();

        return new PatientLoginResponse(
                token,
                tokenService.issueRefreshToken(patient),
                patient.getIdNumber(),
                fullName,
                "PATIENT",
                requiresPinChange
        );
    }

    /**
     * Completes a patient's first sign-in. A temporary PIN cannot be reused as
     * their permanent PIN; neither PIN is written to logs or API responses.
     */
    public PatientLoginResponse changePin(String idNumber, String currentPin, String newPin, String accessToken) {
        Patient patient = patientService.findByIdentifier(idNumber);
        if (patient.getLoginLockedUntil() != null && patient.getLoginLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LoginLockedException("Too many incorrect PIN attempts. Ask the front desk for help or try again later.");
        }
        if (currentPin == null || patient.getPortalPinHash() == null
                || !passwordEncoder.matches(currentPin, patient.getPortalPinHash())) {
            int failures = (patient.getFailedLoginAttempts() == null ? 0 : patient.getFailedLoginAttempts()) + 1;
            patient.setFailedLoginAttempts(failures);
            if (failures >= MAX_FAILED_ATTEMPTS) {
                patient.setLoginLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                patient.setFailedLoginAttempts(0);
            }
            patientRepository.save(patient);
            auditService.recordEvent(null, patient, "PATIENT_PIN_CHANGE_FAILED",
                    "attempted to replace the portal PIN without supplying the correct current PIN", "PATIENT", patient.getId());
            throw new RuntimeException("Current PIN is incorrect");
        }
        if (newPin == null || !newPin.matches("[0-9]{6}")) {
            throw new RuntimeException("New PIN must contain exactly six digits");
        }
        if (currentPin.equals(newPin)) {
            throw new RuntimeException("Choose a different PIN from the temporary PIN");
        }
        // Use the existing PIN validation (reject easily guessed sequential PINs).
        patientService.validatePortalPinForChange(newPin);
        patient.setPortalPinHash(passwordEncoder.encode(newPin));
        patient.setMustChangePortalPin(false);
        patient.setFailedLoginAttempts(0);
        patient.setLoginLockedUntil(null);
        patientRepository.save(patient);
        tokenService.revokePatientRefreshTokens(patient);
        tokenService.revokeAccessToken(accessToken, "PATIENT_PIN_CHANGE");
        auditService.recordEvent(null, patient, "PATIENT_PORTAL_PIN_CHANGED",
                "replaced their temporary portal PIN after signing in", "PATIENT", patient.getId());
        return buildSession(patient);
    }

    public Patient getPatientProfile(String idNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        auditService.recordEvent(null, patient, "PATIENT_VIEW_PROFILE",
                "viewed their patient portal profile", "PATIENT", patient.getId());
        return patient;
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
