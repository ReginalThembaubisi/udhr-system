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
    private AuditService auditService;

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

        Visit saved = visitRepository.save(visit);
        auditService.recordEvent(staff, patient, "REGISTER_VISIT",
                "created visit #" + saved.getId() + " for patient " + patient.getMrn()
                        + " at " + facility.getName(), "VISIT", saved.getId());
        return saved;
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
    /** The patient's latest visit if it's still in progress, else null. */
    public Visit findOpenVisit(Patient patient) {
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId());
        if (!visits.isEmpty() && !TERMINAL_STATUSES.contains(visits.get(0).getStatus())) {
            return visits.get(0);
        }
        return null;
    }

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
        Visit saved = visitRepository.save(visit);
        auditService.recordEvent(staff, patient, "REGISTER_VISIT",
                "opened a clinic visit for patient " + patient.getMrn() + " at " + staff.getFacility().getName(),
                "VISIT", saved.getId());
        return saved;
    }

    public Visit updateStatus(Visit visit, String status) {
        String before = visit.getStatus();
        visit.setStatus(status);
        Visit saved = visitRepository.save(visit);
        if (!java.util.Objects.equals(before, status)) {
            auditService.recordEvent(auditService.currentStaff(), saved.getPatient(), "UPDATE_VISIT_STATUS",
                    "moved visit #" + saved.getId() + " for patient " + saved.getPatient().getMrn()
                            + " from " + before + " to " + status, "VISIT", saved.getId());
        }
        return saved;
    }

    /**
     * The automatic hand-off list behind each role's dashboard: whoever
     * checked the patient in (admin, or the previous stage) moves them into
     * a status, and the next role's queue is simply "every visit at my
     * facility currently sitting in that status" — no ID search needed.
     * Nurse asks for WAITING_VITALS, doctor asks for VITALS_DONE.
     */
    /**
     * One or more statuses, comma-separated: the doctor's queue asks for
     * "VITALS_DONE,DIAGNOSED" so a patient whose diagnosis is saved stays on it
     * until they are sent to the pharmacy or discharged (they used to vanish
     * from the queue as soon as the diagnosis was saved).
     */
    public List<Visit> getFacilityQueue(String staffNumber, String status) {
        Staff staff = staffRepository.findByStaffNumber(staffNumber)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        return java.util.Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .flatMap(s -> visitRepository.findByFacilityIdAndStatusOrderByVisitDateAsc(staff.getFacility().getId(), s).stream())
                .sorted(Comparator.comparing(Visit::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
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

        auditService.recordEvent(staff, patient, "DISCHARGE_PATIENT",
                "discharged patient " + patient.getMrn() + " from visit #" + saved.getId()
                        + " with outcome " + saved.getDischargeOutcome(), "VISIT", saved.getId());
        return saved;
    }
}
