package com.udhr.service;

import com.udhr.dto.DiagnosisRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
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
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    public Diagnosis addDiagnosis(DiagnosisRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff doctor = staffRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        Visit visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setPatient(patient);
        diagnosis.setDoctor(doctor);
        diagnosis.setFacility(facility);
        diagnosis.setVisit(visit);
        diagnosis.setDiagnosis(request.getDiagnosis());
        diagnosis.setNotes(request.getNotes());

        return diagnosisRepository.save(diagnosis);
    }

    public List<Diagnosis> getDiagnosesByPatient(Long patientId) {
        return diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patientId);
    }
}
