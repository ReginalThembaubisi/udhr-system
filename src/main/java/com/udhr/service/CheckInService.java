package com.udhr.service;

import com.udhr.dto.AdminPatientLookupResponse;
import com.udhr.dto.CheckInRequest;
import com.udhr.dto.CheckInSummaryResponse;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.StaffRepository;
import com.udhr.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckInService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    /**
     * Front desk (Admin) checks a patient in — this is paperwork, not a
     * clinical action. It opens the patient's visit in WAITING_VITALS, which
     * is exactly the queue the nurse's dashboard reads from: the patient
     * shows up on the nurse's waiting list automatically, no ID search
     * needed on either side. No vitals, diagnosis, or prescriptions happen
     * here or anywhere else Admin touches.
     */
    /** The visit the patient is now queued under, and whether they were already waiting. */
    public record CheckInResult(Visit visit, boolean alreadyWaiting) {}

    public CheckInResult checkIn(CheckInRequest request, String staffNumber) {
        Patient patient = patientService.findByIdentifier(request.getIdNumber());

        Staff admin = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        String reason = request.getReason() != null ? request.getReason().trim() : "";
        Visit open = visitService.findOpenVisit(patient);
        if (open != null) {
            // Already in the queue. Before, this silently kept the old visit and
            // threw the new reason away while still saying "Checked in". Now the
            // clerk is told, and a new reason replaces the old one.
            if (!reason.isEmpty() && !reason.equals(open.getReason())) {
                open.setReason(reason);
                open = visitRepository.save(open);
            }
            return new CheckInResult(open, true);
        }
        return new CheckInResult(visitService.findOrCreateOpenVisit(patient, admin,
                reason.isEmpty() ? "Front desk check-in" : reason), false);
    }

    /**
     * Admin's record of who has come through the door today, flagging each
     * one as a first-time (new) or returning visitor so front desk always
     * knows who they're dealing with without asking.
     */
    public List<CheckInSummaryResponse> getTodayCheckIns(String staffNumber) {
        Staff admin = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Logged in staff not found"));

        LocalDate today = LocalDate.now();
        List<Visit> todaysVisits = visitRepository.findByFacilityIdOrderByVisitDateDesc(admin.getFacility().getId())
                .stream()
                .filter(v -> v.getVisitDate() != null && v.getVisitDate().toLocalDate().equals(today))
                .collect(Collectors.toList());

        return todaysVisits.stream()
                .map(v -> {
                    long totalVisits = visitRepository.findByPatientIdOrderByVisitDateDesc(v.getPatient().getId()).size();
                    return new CheckInSummaryResponse(v, totalVisits <= 1);
                })
                .collect(Collectors.toList());
    }

    /**
     * Front desk look-up for a patient who isn't on today's list — e.g.
     * confirming someone is already registered, or checking when they were
     * last seen. Deliberately non-clinical: patient demographics and visit
     * history only, nothing a doctor or nurse recorded during a visit.
     */
    public AdminPatientLookupResponse lookupPatient(String identifier) {
        Patient patient = patientService.findByIdentifier(identifier);
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        return new AdminPatientLookupResponse(patient, visits, visits.size() <= 1);
    }
}
