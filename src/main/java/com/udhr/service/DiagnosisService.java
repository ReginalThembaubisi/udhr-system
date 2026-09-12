package com.udhr.service;

import com.udhr.dto.DiagnosisRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DiagnosisService {

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitRepository visitRepository;

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public Diagnosis addDiagnosis(DiagnosisRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // The recording doctor and facility are always the authenticated
        // caller's own -- never trusted from the request body, which would
        // otherwise let a caller forge authorship or cross a facility boundary.
        Staff doctor = currentStaff();
        FacilityGuard.assertSameFacility(doctor, patient);
        Facility facility = doctor.getFacility();

        Visit visit = null;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Visit not found"));
            if (!visit.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("Visit does not belong to this patient");
            }
        } else {
            List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
            if (!visits.isEmpty()) {
                visit = visits.get(0);
            } else {
                visit = new Visit();
                visit.setPatient(patient);
                visit.setStaff(doctor);
                visit.setFacility(facility);
                visit.setReason("Clinical consultation");
                visit.setNotes("Automatically created for clinical diagnosis log.");
                visit = visitRepository.save(visit);
            }
        }

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setPatient(patient);
        diagnosis.setDoctor(doctor);
        diagnosis.setFacility(facility);
        diagnosis.setVisit(visit);
        
        String disease = request.getDiagnosis();
        diagnosis.setDiagnosis(disease);
        diagnosis.setNotes(request.getNotes());

        if (disease != null) {
            String norm = disease.toLowerCase();
            if (norm.contains("diabetes")) {
                diagnosis.setIcd10Code("E11");
            } else if (norm.contains("hypertension")) {
                diagnosis.setIcd10Code("I10");
            } else if (norm.contains("malaria")) {
                diagnosis.setIcd10Code("B50");
            } else if (norm.contains("tuberculosis") || norm.contains("tb")) {
                diagnosis.setIcd10Code("A15");
            } else if (norm.contains("hiv") || norm.contains("aids")) {
                diagnosis.setIcd10Code("B20");
            }
        }

        return diagnosisRepository.save(diagnosis);
    }

    public List<Diagnosis> getDiagnosesByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patientId);
    }
}
