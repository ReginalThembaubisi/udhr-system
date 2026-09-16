package com.udhr.service;

import com.udhr.dto.LabResultRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LabResultService {

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VisitRepository visitRepository;

    public LabResult addLabResult(LabResultRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff staff;
        if (request.getStaffId() != null) {
            staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
        } else {
            String staffNum = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            staff = staffRepository.findByStaffNumber(staffNum)
                    .orElseThrow(() -> new RuntimeException("Logged in staff not found"));
        }

        Facility facility;
        if (request.getFacilityId() != null) {
            facility = facilityRepository.findById(request.getFacilityId())
                    .orElseThrow(() -> new RuntimeException("Facility not found"));
        } else {
            facility = staff.getFacility();
        }

        Visit visit;
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
                visit.setStaff(staff);
                visit.setFacility(facility);
                visit.setReason("Clinical consultation");
                visit.setNotes("Automatically created for lab result log.");
                visit = visitRepository.save(visit);
            }
        }

        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setStaff(staff);
        labResult.setFacility(facility);
        labResult.setVisit(visit);
        labResult.setTestName(request.getTestName());
        labResult.setResult(request.getResult());
        labResult.setUnit(request.getUnit());
        labResult.setNormalRange(request.getNormalRange());
        labResult.setNotes(request.getNotes());

        return labResultRepository.save(labResult);
    }

    public List<LabResult> getLabResultsByPatient(Long patientId) {
        return labResultRepository.findByPatientIdOrderByTestDateDesc(patientId);
    }
}
