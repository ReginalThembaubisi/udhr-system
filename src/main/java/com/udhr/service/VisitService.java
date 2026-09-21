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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private QueueService queueService;

    // A visit is "open" (still eligible to be reused by findOrCreateOpenVisit)
    // as long as it hasn't reached one of these closing states.
    private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETE", "REFERRED", "DISCHARGED");

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
            if (!TERMINAL_STATUSES.contains(latest.getStatus())) {
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

    /**
     * The automatic hand-off list behind each role's dashboard: whoever
     * checked the patient in (admin, or the previous stage) moves them into
     * a status, and the next role's queue is simply "every visit at my
     * facility currently sitting in that status" — no ID search needed.
     * Nurse asks for WAITING_VITALS, doctor asks for VITALS_DONE.
     */
    public List<Visit> getFacilityQueue(String staffNumber, String status) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        return visitRepository.findByFacilityIdAndStatusOrderByVisitDateAsc(staff.getFacility().getId(), status);
    }

    /**
     * The "Today's Queue" board: every one of today's visits at this facility
     * still in progress, across every stage of the pipeline (waiting for
     * vitals, waiting for the doctor, waiting for pharmacy) — a single
     * live view of who's where, oldest arrival first.
     */
    public List<Visit> getTodayFacilityQueue(String staffNumber) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        LocalDate today = LocalDate.now();
        return visitRepository.findByFacilityIdOrderByVisitDateDesc(staff.getFacility().getId()).stream()
                .filter(v -> v.getVisitDate() != null && v.getVisitDate().toLocalDate().equals(today))
                .filter(v -> !TERMINAL_STATUSES.contains(v.getStatus()))
                .sorted(Comparator.comparing(Visit::getVisitDate))
                .collect(Collectors.toList());
    }

    // Explicitly closes out a visit — a clinical decision distinct from the
    // routine WAITING_VITALS -> ... -> COMPLETE dispensing flow, and from a
    // Referral (which closes the visit itself, see ReferralService).
    public Visit discharge(DischargeRequest request, String staffNumber) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        Visit visit;
        if (request.getVisitId() != null) {
            visit = visitRepository.findById(request.getVisitId())
                    .orElseThrow(() -> new RuntimeException("Visit not found"));
        } else {
            visit = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()).stream()
                    .filter(v -> !TERMINAL_STATUSES.contains(v.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No active visit to discharge for this patient"));
        }

        visit.setStatus("DISCHARGED");
        visit.setDischargeOutcome(request.getDischargeOutcome() != null && !request.getDischargeOutcome().isBlank()
                ? request.getDischargeOutcome() : "HOME");
        visit.setDischargeSummary(request.getDischargeSummary());
        if (request.getFollowUpDate() != null && !request.getFollowUpDate().isBlank()) {
            visit.setFollowUpDate(LocalDate.parse(request.getFollowUpDate()));
        }
        visit.setDischargedBy(staff);
        visit.setDischargedAt(LocalDateTime.now());
        Visit saved = visitRepository.save(visit);

        // Same as a referral: the patient is no longer "here" once discharged,
        // so close out any active queue entry at this facility.
        queueService.completeActiveEntryForPatientAtFacility(patient.getId(), staff.getFacility().getId());

        return saved;
    }
}
