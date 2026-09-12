package com.udhr.service;

import com.udhr.dto.VisitRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
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

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public Visit addVisit(VisitRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // The acting staff member and facility are always the authenticated
        // caller's own -- never trusted from the request body, which would
        // otherwise let a caller forge authorship or cross a facility boundary.
        Staff staff = currentStaff();
        FacilityGuard.assertSameFacility(staff, patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStaff(staff);
        visit.setFacility(staff.getFacility());
        visit.setReason(request.getReason());
        visit.setNotes(request.getNotes());

        return visitRepository.save(visit);
    }

    public List<Visit> getVisitsByPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return visitRepository.findByPatientIdOrderByVisitDateDesc(patientId);
    }
}
