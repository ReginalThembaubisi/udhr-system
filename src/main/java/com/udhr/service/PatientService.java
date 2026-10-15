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
