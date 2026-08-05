package com.udhr.service;

import com.udhr.dto.DiagnosisRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    public Diagnosis addDiagnosis(DiagnosisRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff doctor = null;
        if (request.getDoctorId() != null) {
            doctor = staffRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        } else {
            String staffNum = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            doctor = staffRepository.findByStaffNumber(staffNum)
                .orElseThrow(() -> new RuntimeException("Logged in doctor/staff not found"));
        }

        Facility facility = null;
        if (request.getFacilityId() != null) {
            facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));
        } else {
            facility = doctor.getFacility();
        }

        Visit visit = null;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Visit not found"));
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
        return diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patientId);
    }
}
