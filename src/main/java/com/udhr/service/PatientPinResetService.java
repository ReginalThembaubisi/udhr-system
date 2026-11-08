package com.udhr.service;

import com.udhr.exception.LoginLockedException;
import com.udhr.model.DemoEmail;
import com.udhr.model.Patient;
import com.udhr.repository.DemoEmailRepository;
import com.udhr.repository.PatientRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * "Forgot your PIN?" for patients, without a trip to the clinic.
 *
 * ID number and date of birth alone are not enough: both are on the ID card
 * and known to family, so they would let anyone holding someone's ID book
 * open their health record. The patient must also have the cellphone saved on
 * their record: a 6-digit code is texted to it, and only with that code can a
 * new PIN be chosen.
 *
 * - The answer to "send me a code" is always the same, so it can't be used to
 *   find out who is registered or what their number is.
 * - A code lasts 10 minutes, 5 wrong tries cancel it, and only its hash is
 *   stored. One code a minute per patient; requests per address are limited.
 * - With no SMS service set up (the school demo), the text goes to the demo
 *   inbox that only the system admin can open, never back to the browser.
 */
@Service
public class PatientPinResetService {

    static final int CODE_MINUTES = 10;
    static final int MAX_CODE_TRIES = 5;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_REQUESTS_PER_ADDRESS = 10;
    public static final String SENT_MESSAGE = "If those details match a patient with a cellphone number on their record, "
            + "we've sent a 6-digit code to that number. It works for 10 minutes. No code? Go to the front desk at any UDHR clinic "
            + "with your ID and they'll give you a new PIN.";
    private static final String WRONG_CODE = "That code is wrong or has expired. Ask for a new code, or go to the front desk with your ID.";

    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AfricasTalkingService sms;
    private final DemoEmailRepository demoInbox;
    private final AuditService auditService;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final SecureRandom random = new SecureRandom();

    public PatientPinResetService(PatientService patientService, PatientRepository patientRepository,
                                  BCryptPasswordEncoder passwordEncoder, AfricasTalkingService sms,
                                  DemoEmailRepository demoInbox, AuditService auditService, TokenService tokenService,
                                  LoginAttemptService loginAttemptService) {
        this.patientService = patientService;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.sms = sms;
        this.demoInbox = demoInbox;
        this.auditService = auditService;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    /** Step 1: text a code to the cellphone on file. Always answers the same way. */
    public String sendCode(String idNumber, String dateOfBirth, String clientAddress) {
        String addressKey = "pin-reset:" + LoginAttemptService.addressKey(clientAddress);
        Instant locked = loginAttemptService.lockedUntil(addressKey);
        if (locked != null) {
            throw new LoginLockedException("Too many requests. Try again " + LoginAttemptService.waitText(locked)
                    + ", or go to the front desk with your ID.");
        }
        if (loginAttemptService.recordFailure(addressKey, MAX_REQUESTS_PER_ADDRESS)) {
            auditService.recordSecurityEvent(null, "PATIENT_PIN_RESET_LIMITED",
                    "Forgot-PIN requests paused for 15 minutes for address " + clientAddress);
        }

        Patient patient = match(idNumber, dateOfBirth);
        if (patient == null || !hasCellphone(patient)) {
            return SENT_MESSAGE;
        }
        LocalDateTime now = LocalDateTime.now();
        if (patient.getPinResetSentAt() != null && patient.getPinResetSentAt().isAfter(now.minusSeconds(RESEND_SECONDS))) {
            return SENT_MESSAGE; // one a minute: the code already sent still works
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        patient.setPinResetCodeHash(passwordEncoder.encode(code));
        patient.setPinResetExpiresAt(now.plusMinutes(CODE_MINUTES));
        patient.setPinResetAttempts(0);
        patient.setPinResetSentAt(now);
        patientRepository.save(patient);

        String text = "UDHR: your code to reset your PIN is " + code + ". It works for " + CODE_MINUTES
                + " minutes. Don't share it with anyone, not even clinic staff.";
        if (sms.configured()) {
            sms.sendSMS(patient.getContactNumber(), text);
        } else {
            DemoEmail demo = new DemoEmail();
            demo.setKind("SMS");
            demo.setRecipients("Cellphone " + AfricasTalkingService.mask(patient.getContactNumber().trim()));
            demo.setSubject("Text message: PIN reset code");
            demo.setBody(text);
            demoInbox.save(demo);
        }
        auditService.recordEvent(null, patient, "PATIENT_PIN_RESET_CODE_SENT",
                "asked for a PIN reset code, texted to " + AfricasTalkingService.mask(patient.getContactNumber().trim()),
                "PATIENT", patient.getId());
        return SENT_MESSAGE;
    }

    /** Step 2: the code from the text message and a new PIN. */
    public void resetPin(String idNumber, String dateOfBirth, String code, String newPin) {
        Patient patient = match(idNumber, dateOfBirth);
        if (patient == null || patient.getPinResetCodeHash() == null || patient.getPinResetExpiresAt() == null
                || patient.getPinResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(WRONG_CODE);
        }
        if (code == null || !passwordEncoder.matches(code.trim(), patient.getPinResetCodeHash())) {
            int tries = (patient.getPinResetAttempts() == null ? 0 : patient.getPinResetAttempts()) + 1;
            patient.setPinResetAttempts(tries);
            if (tries >= MAX_CODE_TRIES) {
                clearCode(patient);
            }
            patientRepository.save(patient);
            auditService.recordEvent(null, patient, "PATIENT_PIN_RESET_FAILED",
                    "entered a wrong PIN reset code", "PATIENT", patient.getId());
            throw new RuntimeException(WRONG_CODE);
        }
        if (newPin == null || !newPin.matches("[0-9]{6}")) {
            throw new RuntimeException("Your new PIN must be exactly 6 digits");
        }
        patientService.validatePortalPinForChange(newPin);

        patient.setPortalPinHash(passwordEncoder.encode(newPin));
        patient.setMustChangePortalPin(false);
        patient.setFailedLoginAttempts(0);
        patient.setLoginLockedUntil(null);
        clearCode(patient);
        patientRepository.save(patient);
        // Anyone already signed in with the old PIN is signed out.
        tokenService.revokePatientRefreshTokens(patient);
        auditService.recordEvent(null, patient, "PATIENT_PIN_RESET_BY_SMS",
                "chose a new portal PIN using a code texted to their cellphone", "PATIENT", patient.getId());
    }

    private Patient match(String idNumber, String dateOfBirth) {
        if (idNumber == null || idNumber.isBlank() || dateOfBirth == null) {
            return null;
        }
        try {
            Patient patient = patientService.findByIdentifier(idNumber.trim());
            return LocalDate.parse(dateOfBirth.trim()).equals(patient.getDateOfBirth()) ? patient : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasCellphone(Patient patient) {
        String phone = patient.getContactNumber();
        return phone != null && phone.replaceAll("\\D", "").length() >= 9;
    }

    private static void clearCode(Patient patient) {
        patient.setPinResetCodeHash(null);
        patient.setPinResetExpiresAt(null);
        patient.setPinResetAttempts(0);
    }
}
