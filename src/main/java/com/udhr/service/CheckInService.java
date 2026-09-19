package com.udhr.service;

import com.udhr.dto.CheckInRequest;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CheckInService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    /**
     * Front desk (Admin) checks a patient in — this is paperwork, not a
     * clinical action. It just opens the patient's visit early so the nurse
     * can find them by ID number with nothing else to set up. No vitals,
     * diagnosis, or prescriptions happen here or anywhere else Admin touches.
     */
    public Visit checkIn(CheckInRequest request, String staffNumber) {
        Patient patient = patientRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("Patient not found. Register them first."));

        Staff admin = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        return visitService.findOrCreateOpenVisit(patient, admin,
                (request.getReason() != null && !request.getReason().isBlank())
                        ? request.getReason()
                        : "Front desk check-in");
    }

    public List<Visit> getRecentCheckIns(String staffNumber) {
        Staff admin = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));
        return visitRepository.findByFacilityIdOrderByVisitDateDesc(admin.getFacility().getId());
    }
}
