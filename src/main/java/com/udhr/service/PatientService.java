package com.udhr.service;

import com.udhr.dto.PatientRequest;
import com.udhr.exception.PossibleDuplicatePatientException;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PatientService {

    // Bump when the privacy notice shown at registration changes, so each
    // patient's record shows which wording they agreed to.
    public static final String PRIVACY_NOTICE_VERSION = "2026-10";

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
    private AuditService auditService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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

    public Patient findById(Long id) {
        return patientRepository.findById(id)
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

        if (!Boolean.TRUE.equals(request.getConfirmNotDuplicate())
                && request.getFirstName() != null && request.getLastName() != null && request.getDateOfBirth() != null) {
            try {
                java.util.List<Patient> sameNameAndBirth = patientRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                        request.getFirstName().trim(), request.getLastName().trim(), LocalDate.parse(request.getDateOfBirth()));
                if (!sameNameAndBirth.isEmpty()) {
                    throw new PossibleDuplicatePatientException(sameNameAndBirth.get(0));
                }
            } catch (java.time.format.DateTimeParseException ignored) {
                // a bad date is reported by the normal validation below
            }
        }

        if (!Boolean.TRUE.equals(request.getPrivacyNoticeAccepted())) {
            throw new RuntimeException("Explain the privacy notice to the patient and tick that they agreed before registering.");
        }
        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(request.getDateOfBirth());
        } catch (Exception e) {
            throw new RuntimeException("Date of birth is required (YYYY-MM-DD)");
        }
        String consentGivenBy = consentGivenBy(request.getConsentGivenBy(), dateOfBirth);

        Patient patient = new Patient();
        patient.setPrivacyNoticeAcceptedAt(LocalDateTime.now());
        patient.setPrivacyNoticeVersion(PRIVACY_NOTICE_VERSION);
        patient.setConsentGivenBy(consentGivenBy);
        patient.setSmsRemindersConsent(Boolean.TRUE.equals(request.getSmsRemindersConsent()));
        patient.setMrn(generateMrn());
        patient.setIdNumber(request.getIdNumber() != null && !request.getIdNumber().isBlank() ? request.getIdNumber() : null);
        patient.setPassportNumber(request.getPassportNumber() != null && !request.getPassportNumber().isBlank() ? request.getPassportNumber() : null);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());

        patient.setNextOfKinFirstName(request.getNextOfKinFirstName());
        patient.setNextOfKinLastName(request.getNextOfKinLastName());
        patient.setNextOfKinRelationship(request.getNextOfKinRelationship());
        patient.setNextOfKinPhone(request.getNextOfKinPhone());

        if (request.getMotherIdNumber() != null && !request.getMotherIdNumber().isBlank()) {
            patient.setMotherPatient(findByIdentifier(request.getMotherIdNumber()));
        }
        if (request.getBirthFacilityId() != null) {
            facilityRepository.findById(request.getBirthFacilityId()).ifPresent(patient::setBirthFacility);
        }
        patient.setBirthWeightGrams(request.getBirthWeightGrams());
        patient.setBirthLengthCm(request.getBirthLengthCm());
        patient.setApgarScore1Min(request.getApgarScore1Min());
        patient.setApgarScore5Min(request.getApgarScore5Min());

        if (request.getPortalPin() != null && !request.getPortalPin().isBlank()) {
            patient.setPortalPinHash(passwordEncoder.encode(validatePortalPin(request.getPortalPin())));
        }

        Patient savedPatient = patientRepository.save(patient);

        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        auditService.recordCreate(staff, savedPatient, "REGISTER_PATIENT",
                "Registered new patient: " + savedPatient.getMrn(),
                savedPatient.getFirstName() + " " + savedPatient.getLastName()
                        + (savedPatient.getDateOfBirth() != null ? ", born " + savedPatient.getDateOfBirth() : ""));

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

        // Viewing a full clinical record is itself worth recording: the trail has
        // to answer who *looked at* a patient's data, not only who changed it.
        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        auditService.recordView(staff, patient, "VIEW_RECORD",
                "Viewed full record of patient: " + patient.getMrn());

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

    /**
     * Front desk sets or resets a patient's portal PIN after checking their
     * identity in person (e.g. the patient forgot it). Also clears any lockout.
     */
    public Patient setPortalPin(String identifier, String pin, String staffNumber) {
        Patient patient = findByIdentifier(identifier);
        boolean hadPin = patient.getPortalPinHash() != null;
        patient.setPortalPinHash(passwordEncoder.encode(validatePortalPin(pin)));
        patient.setFailedLoginAttempts(0);
        patient.setLoginLockedUntil(null);
        Patient saved = patientRepository.save(patient);

        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        auditService.recordChange(staff, saved, hadPin ? "RESET_PORTAL_PIN" : "SET_PORTAL_PIN",
                (hadPin ? "Reset" : "Set") + " the patient portal PIN for " + saved.getMrn(),
                hadPin ? "PIN set" : "No PIN", "PIN set");
        return saved;
    }

    /**
     * Records consent for a patient registered before consent was captured, or
     * updates it (for example a guardian's details, or a change of mind about
     * SMS reminders). Done by the front desk with the patient present.
     */
    public Patient recordConsent(String identifier, String givenBy, Boolean smsRemindersConsent, String staffNumber) {
        Patient patient = findByIdentifier(identifier);
        String before = describeConsent(patient);
        patient.setConsentGivenBy(consentGivenBy(givenBy, patient.getDateOfBirth()));
        patient.setPrivacyNoticeAcceptedAt(LocalDateTime.now());
        patient.setPrivacyNoticeVersion(PRIVACY_NOTICE_VERSION);
        patient.setSmsRemindersConsent(Boolean.TRUE.equals(smsRemindersConsent));
        Patient saved = patientRepository.save(patient);

        auditService.recordChange(staffRepository.findByStaffNumber(staffNumber).orElse(null), saved, "RECORD_CONSENT",
                "Recorded privacy notice consent for " + saved.getMrn(), before, describeConsent(saved));
        return saved;
    }

    /** A patient switching SMS reminders on or off from the portal (POPIA section 11(2)). */
    public Patient setSmsRemindersConsent(String idNumber, boolean consent) {
        Patient patient = findByIdentifier(idNumber);
        String before = describeConsent(patient);
        patient.setSmsRemindersConsent(consent);
        Patient saved = patientRepository.save(patient);
        auditService.recordChange(null, saved, "PATIENT_SMS_CONSENT",
                "Patient turned SMS reminders " + (consent ? "on" : "off"), before, describeConsent(saved));
        return saved;
    }

    // A child can't consent alone (POPIA section 35): a parent or guardian is named.
    private static String consentGivenBy(String givenBy, LocalDate dateOfBirth) {
        String trimmed = givenBy == null ? "" : givenBy.trim();
        boolean child = dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now().minusYears(18));
        if (child && (trimmed.isEmpty() || "patient".equalsIgnoreCase(trimmed))) {
            throw new RuntimeException("This patient is under 18: enter the parent or guardian who agreed (e.g. Mother: Thandi Zulu).");
        }
        if (trimmed.length() > 120) {
            throw new RuntimeException("Keep the name of the person who agreed under 120 characters");
        }
        return trimmed.isEmpty() ? "Patient" : trimmed;
    }

    private static String describeConsent(Patient patient) {
        if (patient.getPrivacyNoticeAcceptedAt() == null) {
            return "No consent recorded";
        }
        return "Notice " + patient.getPrivacyNoticeVersion() + " agreed by " + patient.getConsentGivenBy()
                + "; SMS reminders " + (Boolean.TRUE.equals(patient.getSmsRemindersConsent()) ? "on" : "off");
    }

    private String validatePortalPin(String pin) {
        String trimmed = pin == null ? "" : pin.trim();
        if (!trimmed.matches("\\d{6}")) {
            throw new RuntimeException("Portal PIN must be exactly 6 digits");
        }
        if (trimmed.chars().distinct().count() == 1 || "123456".equals(trimmed) || "654321".equals(trimmed)) {
            throw new RuntimeException("Choose a PIN that isn't easy to guess (not 123456 or the same digit six times)");
        }
        return trimmed;
    }
}
