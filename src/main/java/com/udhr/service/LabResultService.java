package com.udhr.service;

import com.udhr.dto.LabResultRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
import com.udhr.security.CurrentUser;
import com.udhr.security.FacilityGuard;
import org.springframework.beans.factory.annotation.Autowired;
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
    private VisitRepository visitRepository;

    private Staff currentStaff() {
        return staffRepository.findByStaffNumber(CurrentUser.principal())
                .orElseThrow(() -> new RuntimeException("Authenticated staff not found"));
    }

    public LabResult addLabResult(LabResultRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // The recording staff member and facility are always the authenticated
        // caller's own -- never trusted from the request body.
        Staff staff = currentStaff();
        FacilityGuard.assertSameFacility(staff, patient);
        Facility facility = staff.getFacility();

        Visit visit;
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
                visit.setStaff(staff);
                visit.setFacility(facility);
                visit.setReason("Clinical consultation");
                visit.setNotes("Automatically created for lab result entry.");
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
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        FacilityGuard.assertSameFacility(currentStaff(), patient);
        return labResultRepository.findByPatientIdOrderByTestDateDesc(patientId);
    }
}
