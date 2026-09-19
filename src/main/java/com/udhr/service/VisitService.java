package com.udhr.service;

import com.udhr.dto.VisitRequest;
import com.udhr.model.Facility;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.FacilityRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VisitService {

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    public Visit addVisit(VisitRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new RuntimeException("Facility not found"));

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(staff);
        visit.setFacility(facility);
        visit.setReason(request.getReason());
        visit.setNotes(request.getNotes());

        return visitRepository.save(visit);
    }

    public List<Visit> getVisitsByPatient(Long patientId) {
        return visitRepository.findByPatientIdOrderByVisitDateDesc(patientId);
    }

    /**
     * Finds the patient's current open visit (any status other than COMPLETE),
     * or opens a new one if there isn't one. This is what lets a nurse, doctor
     * or pharmacist all land on the same visit just by looking the patient up
     * by ID number, with no queue ticket needed.
     */
    public Visit findOrCreateOpenVisit(Patient patient, Staff staff, String reason) {
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        if (!visits.isEmpty()) {
            Visit latest = visits.get(0);
            if (!"COMPLETE".equals(latest.getStatus())) {
                return latest;
            }
        }

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(staff);
        visit.setFacility(staff.getFacility());
        visit.setReason(reason != null && !reason.isBlank() ? reason : "Clinic visit");
        return visitRepository.save(visit);
    }

    public Visit updateStatus(Visit visit, String status) {
        visit.setStatus(status);
        return visitRepository.save(visit);
    }
}
