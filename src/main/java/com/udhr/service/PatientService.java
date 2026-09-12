package com.udhr.service;

import com.udhr.dto.PatientRequest;
import com.udhr.dto.PatientRecordResponse;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.FacilityGuard;
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

    public Patient findByIdNumber(String idNumber, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
        FacilityGuard.assertSameFacility(staff, patient);
        return patient;
    }

    public Patient registerPatient(PatientRequest request, String staffNumber) {
        if (patientRepository.existsByIdNumber(request.getIdNumber())) {
            throw new RuntimeException("Patient already registered");
        }

        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));

        Patient patient = new Patient();
        patient.setIdNumber(request.getIdNumber());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setAddress(request.getAddress());
        // The registering staff member's own facility is authoritative -- a
        // caller cannot register a patient into a different facility.
        patient.setFacility(staff.getFacility());

        Patient savedPatient = patientRepository.save(patient);

        // Log the register action in AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setStaff(staff);
        auditLog.setPatient(savedPatient);
        auditLog.setAction("REGISTER_PATIENT");
        auditLog.setDescription("Registered new patient: " + request.getIdNumber());
        auditLogRepository.save(auditLog);

        return savedPatient;
    }

    public PatientRecordResponse getFullRecord(String idNumber, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
        FacilityGuard.assertSameFacility(staff, patient);

        List<Allergy> allergies = allergyRepository.findByPatientId(patient.getId());
        List<ChronicCondition> chronicConditions = chronicConditionRepository.findByPatientId(patient.getId());
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patient.getId());
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<LabResult> labResults = labResultRepository.findByPatientIdOrderByTestDateDesc(patient.getId());

        // Log the view action in AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setStaff(staff);
        auditLog.setPatient(patient);
        auditLog.setAction("VIEW_RECORD");
        auditLog.setDescription("Viewed full record of patient: " + idNumber);
        auditLogRepository.save(auditLog);

        return new PatientRecordResponse(
                patient,
                allergies,
                chronicConditions,
                visits,
                diagnoses,
                prescriptions,
                labResults
        );
    }
}
