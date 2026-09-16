package com.udhr.service;

import com.udhr.dto.PatientRequest;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

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
    private FacilityRepository facilityRepository;

    @Autowired
    private ImmunizationRepository immunizationRepository;

    @Autowired
    private ImmunizationService immunizationService;

    @Autowired
    private VitalsRepository vitalsRepository;

    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient findByIdNumber(String idNumber) {
        return patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient findByUhid(String uhid) {
        return patientRepository.findByUhid(uhid)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient registerPatient(PatientRequest request, String staffNumber) {
        boolean hasIdNumber = request.getIdNumber() != null && !request.getIdNumber().isBlank();
        boolean hasPassport = request.getPassportNumber() != null && !request.getPassportNumber().isBlank();

        if (hasIdNumber && patientRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("Patient already registered");
        }
        if (hasPassport && patientRepository.findByPassportNumber(request.getPassportNumber()).isPresent()) {
            throw new RuntimeException("Patient already registered");
        }

        Patient patient = new Patient();
        // A national ID or passport number is not required at registration time:
        // newborns and undocumented patients still need a file. The permanent
        // identifier for every patient is the UHID generated on save.
        patient.setIdNumber(hasIdNumber ? request.getIdNumber() : null);
        patient.setPassportNumber(hasPassport ? request.getPassportNumber() : null);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setAddress(request.getAddress());

        if (request.getMotherIdNumber() != null && !request.getMotherIdNumber().isBlank()) {
            patientRepository.findByIdNumber(request.getMotherIdNumber())
                    .ifPresent(patient::setMotherPatient);
        }
        if (request.getBirthFacilityId() != null) {
            facilityRepository.findById(request.getBirthFacilityId())
                    .ifPresent(patient::setBirthFacility);
        }
        patient.setBirthWeightGrams(request.getBirthWeightGrams());
        patient.setBirthLengthCm(request.getBirthLengthCm());
        patient.setApgarScore1Min(request.getApgarScore1Min());
        patient.setApgarScore5Min(request.getApgarScore5Min());

        Patient savedPatient = patientRepository.save(patient);

        // A registration carrying birth details (weight/length/Apgar, or a
        // linked mother) means this is a newborn's file: generate their EPI
        // immunization schedule immediately, since doses start at birth.
        boolean isBirthRegistration = savedPatient.getMotherPatient() != null
                || savedPatient.getBirthWeightGrams() != null
                || savedPatient.getBirthFacility() != null;
        if (isBirthRegistration) {
            immunizationService.generateEpiSchedule(savedPatient);
        }

        // Log the register action in AuditLog
        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            AuditLog auditLog = new AuditLog();
            auditLog.setStaff(staff);
            auditLog.setPatient(savedPatient);
            auditLog.setAction("REGISTER_PATIENT");
            auditLog.setDescription("Registered new patient: " + savedPatient.getUhid()
                    + (hasIdNumber ? " (ID: " + request.getIdNumber() + ")" : " (no ID number yet)"));
            auditLogRepository.save(auditLog);
        }

        return savedPatient;
    }

    public PatientRecordResponse getFullRecord(String idNumber, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return buildFullRecord(patient, staffNumber, "Viewed full record of patient: " + idNumber);
    }

    public PatientRecordResponse getFullRecordByUhid(String uhid, String staffNumber) {
        Patient patient = patientRepository.findByUhid(uhid)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return buildFullRecord(patient, staffNumber, "Viewed full record of patient: " + uhid);
    }

    private PatientRecordResponse buildFullRecord(Patient patient, String staffNumber, String auditDescription) {
        List<Allergy> allergies = allergyRepository.findByPatientId(patient.getId());
        List<ChronicCondition> chronicConditions = chronicConditionRepository.findByPatientId(patient.getId());
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patient.getId());
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<LabResult> labResults = labResultRepository.findByPatientIdOrderByTestDateDesc(patient.getId());
        List<Immunization> immunizations = immunizationRepository.findByPatientIdOrderByScheduledDateAsc(patient.getId());
        List<Vitals> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());

        // Log the view action in AuditLog
        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            AuditLog auditLog = new AuditLog();
            auditLog.setStaff(staff);
            auditLog.setPatient(patient);
            auditLog.setAction("VIEW_RECORD");
            auditLog.setDescription(auditDescription);
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
                immunizations,
                vitals
        );
    }
}
