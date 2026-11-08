package com.udhr.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udhr.model.Patient;
import com.udhr.model.Referral;
import com.udhr.model.Staff;
import com.udhr.model.Visit;
import com.udhr.repository.AuditLogRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.ReferralRepository;
import com.udhr.repository.VisitRepository;
import com.udhr.service.AuditService;
import com.udhr.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Break the glass": a doctor or nurse can open any patient in the network,
 * but opening one who isn't on their clinic's list today needs a reason. The
 * reason goes into the audit trail (ACCESS_OUTSIDE_QUEUE), so looking up a
 * neighbour or a celebrity leaves a record of why, and the security alerts
 * can point at it. Checked here on the server, before the record is sent, so
 * it can't be skipped by calling the API directly.
 *
 * On today's list: a visit at the staff member's clinic today, a visit there
 * that is still open, or a recent referral to or from that clinic. A reason
 * given once covers that patient for that staff member for 12 hours.
 *
 * The browser sends the reason in the X-Access-Reason header after the
 * server answers 428 with {"reasonRequired": true}.
 */
@Component
public class AccessReasonInterceptor implements HandlerInterceptor {

    public static final String ACTION = "ACCESS_OUTSIDE_QUEUE";
    public static final String HEADER = "X-Access-Reason";
    private static final int GRANT_HOURS = 12;
    private static final Set<String> ROLES = Set.of("DOCTOR", "NURSE");
    private static final Set<String> FINISHED = Set.of("COMPLETE", "REFERRED", "DISCHARGED", "CANCELLED");

    // /api/patients/<identifier> and /api/patients/<id>/record
    private static final Pattern PATIENTS = Pattern.compile("^/api/patients/([^/]+)(/record)?$");
    // /api/<area>/patient/<id>[/...]: vitals, allergies, labs, visits, referrals...
    private static final Pattern BY_PATIENT_ID = Pattern.compile("^/api/([a-z-]+)/patient/(\\d+)(/.*)?$");

    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final VisitRepository visitRepository;
    private final ReferralRepository referralRepository;
    private final ObjectMapper json = new ObjectMapper();

    public AccessReasonInterceptor(AuditService auditService, AuditLogRepository auditLogRepository,
                                   PatientRepository patientRepository, PatientService patientService,
                                   VisitRepository visitRepository, ReferralRepository referralRepository) {
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
        this.visitRepository = visitRepository;
        this.referralRepository = referralRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Every method: a POST to /api/<area>/patient/<id> (e.g. evaluating
        // clinical alerts) returns that patient's data too.
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        Patient patient = patientFor(request.getRequestURI());
        if (patient == null) {
            return true; // not a patient read, or no such patient: the endpoint answers
        }
        Staff staff = auditService.currentStaff();
        if (staff == null || !ROLES.contains(staff.getRole()) || staff.getFacility() == null) {
            return true;
        }
        if (onTodaysList(patient, staff) || recentlyGranted(patient, staff)) {
            return true;
        }

        String reason = reasonFrom(request);
        if (reason != null) {
            auditService.recordEvent(staff, patient, ACTION,
                    "opened the record of patient " + patient.getMrn() + ", who is not on today's list at "
                            + staff.getFacility().getName() + ". Reason: " + reason,
                    "PATIENT", patient.getId());
            return true;
        }

        response.setStatus(428); // Precondition Required
        response.setContentType("application/json");
        response.getWriter().write(json.writeValueAsString(Map.of(
                "reasonRequired", true,
                "message", "This patient isn't on today's list at your clinic. Give a reason to open their record; it is saved in the audit trail.")));
        return false;
    }

    private Patient patientFor(String path) {
        try {
            Matcher byId = BY_PATIENT_ID.matcher(path);
            if (byId.matches()) {
                if ("pharmacy".equals(byId.group(1))) {
                    return null;
                }
                return patientRepository.findById(Long.parseLong(byId.group(2))).orElse(null);
            }
            Matcher patients = PATIENTS.matcher(path);
            if (patients.matches()) {
                // ID number, file number or passport, as PatientController reads it.
                // (An SA ID number is all digits: it is not a database id.)
                return patientService.findByIdentifier(patients.group(1));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private boolean onTodaysList(Patient patient, Staff staff) {
        Long facilityId = staff.getFacility().getId();
        LocalDate today = LocalDate.now();
        for (Visit visit : visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId())) {
            if (visit.getFacility() == null || !facilityId.equals(visit.getFacility().getId())) {
                continue;
            }
            boolean todays = visit.getVisitDate() != null && visit.getVisitDate().toLocalDate().equals(today);
            if (todays || !FINISHED.contains(visit.getStatus())) {
                return true;
            }
        }
        LocalDateTime recent = LocalDateTime.now().minusDays(30);
        for (Referral referral : referralRepository.findByPatientIdOrderByReferredAtDesc(patient.getId())) {
            if (referral.getReferredAt() == null || referral.getReferredAt().isBefore(recent)) {
                continue;
            }
            boolean toUs = referral.getToFacility() != null && facilityId.equals(referral.getToFacility().getId());
            boolean fromUs = referral.getFromFacility() != null && facilityId.equals(referral.getFromFacility().getId());
            if (toUs || fromUs) {
                return true;
            }
        }
        return false;
    }

    private boolean recentlyGranted(Patient patient, Staff staff) {
        return auditLogRepository.existsByStaffIdAndPatientIdAndActionAndCreatedAtAfter(
                staff.getId(), patient.getId(), ACTION, LocalDateTime.now().minusHours(GRANT_HOURS));
    }

    private static String reasonFrom(HttpServletRequest request) {
        String raw = request.getHeader(HEADER);
        if (raw == null) {
            return null;
        }
        String reason = URLDecoder.decode(raw, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        if (reason.length() < 3) {
            return null;
        }
        return reason.length() > 200 ? reason.substring(0, 200) : reason;
    }
}
