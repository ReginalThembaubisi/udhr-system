package com.udhr.service;

import com.udhr.dto.PrescriptionRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    public Prescription addPrescription(PrescriptionRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff doctor = staffRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        Visit visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setFacility(facility);
        prescription.setVisit(visit);
        prescription.setMedication(request.getMedication());
        prescription.setDosage(request.getDosage());
        prescription.setFrequency(request.getFrequency());
        prescription.setStartDate(LocalDate.parse(request.getStartDate()));
        prescription.setEndDate(LocalDate.parse(request.getEndDate()));
        prescription.setNotes(request.getNotes());
        prescription.setActive(true);

        return prescriptionRepository.save(prescription);
    }

    public List<Prescription> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<Prescription> getActivePrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
    }
}
