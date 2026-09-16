package com.udhr.service;

import com.udhr.dto.MedicationPrescriptionTally;
import com.udhr.dto.PrescriberTally;
import com.udhr.dto.PrescriptionReportResponse;
import com.udhr.dto.PrescriptionRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public PrescriptionReportResponse getReport(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        Facility facility = staff.getFacility();

        List<Prescription> all = prescriptionRepository.findByFacilityIdOrderByCreatedAtDesc(facility.getId());

        int activeCount = (int) all.stream().filter(Prescription::getActive).count();

        LocalDate today = LocalDate.now();
        int issuedToday = (int) all.stream()
                .filter(p -> p.getCreatedAt().toLocalDate().equals(today))
                .count();

        long uniquePatients = all.stream()
                .map(p -> p.getPatient().getId())
                .distinct()
                .count();

        Map<String, Integer> medicationCounts = new LinkedHashMap<>();
        Map<String, Integer> prescriberCounts = new LinkedHashMap<>();
        for (Prescription p : all) {
            medicationCounts.merge(p.getMedication(), 1, Integer::sum);
            String prescriberName = p.getDoctor().getFirstName() + " " + p.getDoctor().getLastName();
            prescriberCounts.merge(prescriberName, 1, Integer::sum);
        }

        List<MedicationPrescriptionTally> topMedications = medicationCounts.entrySet().stream()
                .map(e -> new MedicationPrescriptionTally(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(MedicationPrescriptionTally::getPrescriptionCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<PrescriberTally> topPrescribers = prescriberCounts.entrySet().stream()
                .map(e -> new PrescriberTally(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(PrescriberTally::getPrescriptionCount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<Prescription> recent = all.stream().limit(10).collect(Collectors.toList());

        return new PrescriptionReportResponse(
                facility.getName(),
                all.size(),
                activeCount,
                issuedToday,
                (int) uniquePatients,
                topMedications,
                topPrescribers,
                recent
        );
    }
}
