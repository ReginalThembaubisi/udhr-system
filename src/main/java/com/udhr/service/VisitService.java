package com.udhr.service;

import com.udhr.dto.DischargeRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public Visit discharge(DischargeRequest request, String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        Visit visit;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                    .orElseThrow(() -> new RuntimeException("Visit not found"));
        } else {
            visit = visitRepository.findByPatientIdOrderByVisitDateDesc(request.getPatientId()).stream()
                    .filter(v -> v.getStatus() == Visit.Status.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No active visit to discharge for this patient"));
        }

        visit.setStatus(Visit.Status.DISCHARGED);
        visit.setDischargeOutcome(Visit.DischargeOutcome.valueOf(request.getDischargeOutcome()));
        visit.setDischargeSummary(request.getDischargeSummary());
        if (request.getFollowUpDate() != null && !request.getFollowUpDate().isBlank()) {
            visit.setFollowUpDate(LocalDate.parse(request.getFollowUpDate()));
        }
        visit.setDischargedBy(staff);
        visit.setDischargedAt(LocalDateTime.now());

        return visitRepository.save(visit);
    }
}
