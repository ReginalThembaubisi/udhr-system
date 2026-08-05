package com.udhr.service;

import com.udhr.dto.PrescriptionRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private ReminderService reminderService;

    public Prescription addPrescription(PrescriptionRequest request) {
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
                visit.setNotes("Automatically created for prescription issue.");
                visit = visitRepository.save(visit);
            }
        }

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setFacility(facility);
        prescription.setVisit(visit);
        String medName = request.getMedicationName() != null ? request.getMedicationName() : request.getMedication();
        prescription.setMedication(medName);

        // Robust date handling
        LocalDate start = LocalDate.now();
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            start = LocalDate.parse(request.getStartDate());
        }
        prescription.setStartDate(start);

        int duration = (request.getDurationDays() != null && request.getDurationDays() > 0) ? request.getDurationDays() : 7;
        LocalDate end = start.plusDays(duration);
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            end = LocalDate.parse(request.getEndDate());
        }
        prescription.setEndDate(end);

        prescription.setDosage(request.getDosage());
        prescription.setFrequency(request.getFrequency());
        prescription.setNotes(request.getNotes());
        prescription.setActive(true);

        Prescription saved = prescriptionRepository.save(prescription);
        try {
            reminderService.createRemindersForPrescription(saved);
        } catch (Exception e) {
            System.err.println("Failed to create reminders: " + e.getMessage());
        }
        return saved;
    }

    public List<Prescription> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<Prescription> getActivePrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
    }
}
