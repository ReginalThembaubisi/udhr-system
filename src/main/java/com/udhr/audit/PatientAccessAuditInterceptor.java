package com.udhr.audit;

import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.PatientRepository;
import com.udhr.service.AuditService;
import com.udhr.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes an audit entry whenever a staff member reads a patient's data, so a
 * system administrator can answer "who looked at this patient's record?"
 * (POPIA section 19, and the accountability condition in section 8).
 *
 * Done here, once, rather than in each of the ~20 endpoints that return a
 * patient's vitals, allergies, labs, visits and so on, so a new endpoint is
 * covered without anyone remembering to add logging. Three reads already write
 * their own, more detailed entry and are skipped: the doctor's full record,
 * the pharmacy lookup and the patient's own portal.
 *
 * Opening one patient loads several of these at once; repeat reads of the same
 * area of the same patient by the same person within 10 minutes are written once.
 */
@Component
public class PatientAccessAuditInterceptor implements HandlerInterceptor {

    // /api/<area>/patient/<id>[/...] — vitals, allergies, labs, visits, referrals...
    private static final Pattern BY_PATIENT_ID = Pattern.compile("^/api/([a-z-]+)/patient/(\\d+)(/.*)?$");
    // /api/patients/<identifier> (not /record) and the front desk's check-in lookup
    private static final Pattern BY_IDENTIFIER = Pattern.compile("^/api/(patients|checkin/lookup)/([^/]+)$");
    private static final Duration REPEAT_WINDOW = Duration.ofMinutes(10);

    private final AuditService auditService;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final Map<String, Instant> recentlyLogged = new ConcurrentHashMap<>();

    public PatientAccessAuditInterceptor(AuditService auditService, PatientRepository patientRepository,
                                         PatientService patientService) {
        this.auditService = auditService;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"GET".equals(request.getMethod()) || response.getStatus() != 200 || ex != null) {
            return;
        }
        String path = request.getRequestURI();
        Patient patient = null;
        String area = null;
        try {
            Matcher byId = BY_PATIENT_ID.matcher(path);
            Matcher byIdentifier = BY_IDENTIFIER.matcher(path);
            if (byId.matches() && !"pharmacy".equals(byId.group(1))) {
                area = byId.group(1) + (byId.group(3) != null ? byId.group(3) : "");
                patient = patientRepository.findById(Long.parseLong(byId.group(2))).orElse(null);
            } else if (byIdentifier.matches()) {
                area = "patients".equals(byIdentifier.group(1)) ? "demographics" : "check-in lookup";
                patient = patientService.findByIdentifier(byIdentifier.group(2));
            }
        } catch (Exception e) {
            return; // unknown patient: nothing was shown
        }
        if (patient == null) {
            return;
        }

        Staff staff = auditService.currentStaff();
        if (staff == null) {
            return; // a patient reading their own data, logged by the portal itself
        }
        String key = staff.getId() + ":" + patient.getId() + ":" + area;
        Instant now = Instant.now();
        Instant last = recentlyLogged.get(key);
        if (last != null && last.plus(REPEAT_WINDOW).isAfter(now)) {
            return;
        }
        recentlyLogged.put(key, now);
        if (recentlyLogged.size() > 10_000) {
            recentlyLogged.entrySet().removeIf(e -> e.getValue().plus(REPEAT_WINDOW).isBefore(now));
        }
        auditService.recordView(staff, patient, "VIEW_PATIENT_DATA",
                "Viewed " + area + " for " + patient.getMrn());
    }
}
