package com.udhr.service;

import com.udhr.dto.LabResultRequest;
import com.udhr.model.*;
import com.udhr.repository.*;
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
    private VisitService visitService;

    /**
     * Staff (nurse or doctor) looks the patient up by ID number and pastes the
     * result straight in — same "find patient, act on their current visit"
     * pattern as vitals, so nobody needs to know a visit ID or facility ID.
     */
    public LabResult addLabResult(LabResultRequest request, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        Visit visit = visitService.findOrCreateOpenVisit(patient, staff, "Lab test");

        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setStaff(staff);
        labResult.setFacility(staff.getFacility());
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
