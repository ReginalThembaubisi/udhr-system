package com.udhr.service;

import com.udhr.dto.PrescriptionRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
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
    private VisitRepository visitRepository;

    @Autowired
    private ReminderService reminderService;

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public Prescription addPrescription(PrescriptionRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // The prescribing doctor and facility are always the authenticated
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
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<Prescription> getActivePrescriptionsByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
    }
}
